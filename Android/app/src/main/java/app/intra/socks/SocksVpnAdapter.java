package app.intra.socks;

import com.google.firebase.crash.FirebaseCrash;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import androidx.annotation.NonNull;
import app.intra.DnsVpnService;
import app.intra.LocalhostResolver;
import app.intra.VpnAdapter;
import app.intra.util.LogWrapper;
import app.intra.util.SafeTun2Socks;
import sockslib.common.methods.NoAuthenticationRequiredMethod;

/**
 * This is a VpnAdapter that captures all traffic and routes it through a local SOCKS server.
 */
public class SocksVpnAdapter extends VpnAdapter {
  private static final String LOG_TAG = "SocksVpnAdapter";

  // IPv4 VPN constants
  private static final String IPV4_TEMPLATE = "10.111.222.%d";
  private static final int IPV4_PREFIX_LENGTH = 24;
  private static final String IPV4_NETMASK = "255.255.255.0";

  // Randomly generated unique local IPv6 unicast subnet prefix, as defined by RFC 4193.
  private static final String IPV6_TEMPLATE = "fd66:f83a:c650::%d";
  private static final int IPV6_PREFIX_LENGTH = 120;

  // Disable Transparent DNS.  We need Transparent DNS to be disabled because it is not compatible
  // with standard SOCKS-UDP servers, only special servers that use a constant UDP port.
  private static final int TRANSPARENT_DNS_ENABLED = 0;
  private static final int SOCKS5_UDP_ENABLED = 1;

  // The VPN service and tun2socks must agree on the layout of the network.  By convention, we
  // assign the following values to the final byte of an address within a subnet.
  private enum LanIp {
    GATEWAY(1), ROUTER(2), DNS(3);

    // Value of the final byte, to be substituted into the template.
    private final int value;

    LanIp(int value) {
      this.value = value;
    }

    String make(String template) {
      return String.format(Locale.ROOT, template, value);
    }
  }

  // DNS resolver running on localhost.
  private LocalhostResolver resolver;

  // TUN device representing the VPN.
  private ParcelFileDescriptor tunFd;

  public static SocksVpnAdapter establish(@NonNull DnsVpnService vpnService) {
    LocalhostResolver resolver = LocalhostResolver.get(vpnService);
    if (resolver == null) {
      return null;
    }
    ParcelFileDescriptor tunFd = establishVpn(vpnService);
    if (tunFd == null) {
      return null;
    }
    return new SocksVpnAdapter(tunFd, resolver);
  }

  private SocksVpnAdapter(ParcelFileDescriptor tunFd, LocalhostResolver resolver) {
    super(LOG_TAG);
    this.resolver = resolver;
    this.tunFd = tunFd;
  }

  @Override
  public void run() {
    resolver.start();

    // VPN parameters
    final String fakeDnsIp = LanIp.DNS.make(IPV4_TEMPLATE);
    final String fakeDnsAddress = fakeDnsIp + ":" + DNS_DEFAULT_PORT;
    final String ipv4Router = LanIp.ROUTER.make(IPV4_TEMPLATE);
    // Disable IPv6 due to a double-free in tun2socks.
    // TODO: Re-enable IPv6 once the double-free in tun2socks is debugged.
    final String ipv6Router = null;

    // Proxy parameters
    InetSocketAddress fakeDns = new InetSocketAddress(fakeDnsIp, DNS_DEFAULT_PORT);
    InetSocketAddress trueDns = resolver.getAddress();
    // Allocate 5 KB per socket.  sockslib's default is 5 MB, which causes OOM crashes.
    final int BUFFER_SIZE = 5 * 1024;
    final InetAddress localhost;
    try {
      localhost = Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
    } catch (UnknownHostException e) {
      LogWrapper.report(e);
      return;
    }

    SocksServer proxy = new SocksServer(fakeDns, trueDns);
    proxy.setSupportMethods(new NoAuthenticationRequiredMethod());
    proxy.setBindAddr(localhost);
    proxy.setBindPort(0);  // Uses our custom SocksServer's dynamic port feature.
    proxy.setBufferSize(BUFFER_SIZE);
    try {
      proxy.start();
    } catch (IOException e) {
      LogWrapper.report(e);
      return;
    }

    // After start(), if bindPort was 0, it has been changed to the dynamically allocated port.
    String proxyAddress = proxy.getBindAddr().getHostAddress() + ":" + proxy.getBindPort();
    SafeTun2Socks tun2Socks = new SafeTun2Socks(tunFd, VPN_INTERFACE_MTU, ipv4Router, IPV4_NETMASK,
        ipv6Router, proxyAddress, proxyAddress, fakeDnsAddress, TRANSPARENT_DNS_ENABLED,
        SOCKS5_UDP_ENABLED);

    try {
      synchronized(this) {
        wait();  // Wait for an interrupt, which is produced by the close() method.
      }
    } catch (InterruptedException e) {
      // close() has been called.  This is as expected.
    }

    tun2Socks.stop();
    proxy.shutdown();
    resolver.shutdown();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private static ParcelFileDescriptor establishVpn(DnsVpnService vpnService) {
    try {
      return vpnService.newBuilder()
          .setSession("Intra tun2socks VPN")
          .setMtu(VPN_INTERFACE_MTU)
          .addAddress(LanIp.GATEWAY.make(IPV4_TEMPLATE), IPV4_PREFIX_LENGTH)
          .addRoute("0.0.0.0", 0)
          .addDnsServer(LanIp.DNS.make(IPV4_TEMPLATE))
          .addAddress(LanIp.GATEWAY.make(IPV6_TEMPLATE), IPV6_PREFIX_LENGTH)
          .addRoute("::", 0)
          .addDisallowedApplication(vpnService.getPackageName())
          .establish();
    } catch (Exception e) {
      FirebaseCrash.report(e);
      return null;
    }
  }

  @Override
  public void close() {
    // Stop the thread.
    interrupt();
    // After the interrupt, the run() method will stop Tun2Socks, and then complete.
    // We need to wait until that is finished before closing the TUN device.
    try {
      join();
    } catch (InterruptedException e) {
      // This is weird: the calling thread has _also_ been interrupted.
      LogWrapper.report(e);
    }

    try {
      tunFd.close();
    } catch (IOException e) {
      LogWrapper.report(e);
    }
  }
}
