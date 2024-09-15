9/12/2024
using *rasberry pi imager* software, I can download the ubuntu image for my PI.
I was able to setup ssh (without keys for now):
`ufw enable` `ufw allow 22` <- on the server this will enable passage through the firewall and allow connection on port 22 which ssh uses to connect by default
`ssh myUser@serverDeviceIP` will startup the conection on the client side, though you can also setup the config file within the .ssh folder to make it more streamlined.

9/14/2024
Lets encrypt uses certbot which you install on your system. This will run a test and give you an SSL certificate

9/15/2024
Lets encrypt can force all http connections to become https. (HSTS) is a security HTTP header to make sure the connection has the cert, otherwise it will terminate it.
