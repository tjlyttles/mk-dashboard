# MK Dashboard

Moqui component for MK Decision's Dashboard.

This component contains two screens mounted under the root screen (webroot.xml, see MoquiConf.xml file):

1. **/custom**: custom application root screen, all screens for the app (like the custom/dashboard screen) go under this screen 
2. **/dashboard**: render wrapper, similar to qapps and vapps, containing the Vue + Quasar SPA shell

The custom.xml screen requires authentication so also requires authorization. The data needed for authorization is in the data/CustomSetupData.xml file and must be loaded to access the app.
 
When rendering screens through dashboard (just like qapps and vapps) the apps root screen (in this case custom.xml) is just a placeholder and is never rendered.
To access the screens under /custom go to /dashboard instead (just like going to /qapps instead of /apps), ie something like:

http://localhost:8080/dashboard
