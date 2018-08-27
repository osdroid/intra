# My Mods

Wouldn't it be cool to have a site blocker, plus your dns lookups protected
at the same time? That's what my fork is all about. I intend it mostly for
me, but well, it started open source, so let's leave it open source! :)

I also plan to add some sites that can be temporarily unblocked, maybe allowing
me a given time per day to access them! (yes, I'm looking at you news sites!).
And even an "all access mode" just in case I plan to do buy something with the phone.

Finally, make it really easy to block new sites from the "recent queries" list.

---

# Intra

Intra is an experimental tool that allows you to test new DNS-over-HTTPS
services that encrypt domain name lookups and prevent manipulation by your
network. It currently supports services from Cloudflare and Google, and
additional options may be added over time.  You can get it from the
Google Play Store [here](https://play.google.com/store/apps/details?id=app.intra).

Features:
* Built-in support for public DNS services from Cloudflare and Google
* Visualization of server performance and application query behavior
* Geocoding of query results to compare against expected regional results

## Android build instructions

1. Clone this repo.
2. Add the keystore.properties file under the Android directory

   The format is like this (just add the information of your android key):  
   keyAlias=  
   keyPassword=  
   storeFile=  
   storePassword=  
3. Open the `Android/` directory in Android Studio 3.0 or later.
4. Connect your phone
5. Click the green "play" triangle button.
