## Global notes:
- I'm using [NoIP](https://my.noip.com/dynamic-dns) as my dns provider


## Things to consider:
- How do I deal with multiple users writing on the same list. **Dissalow a second user to write(lock)**
- Should I require users to login everytime they open the app? (I think no) **MAYBE**
- Should I save/send entire lists or try to find the changes made to a list and send that? **Send entire lists**
- Should I try to load lists in chunks so the user can have something to look at while the rest of the list loads? **no chunking**
- If I load a list once I probably shouldnt reload it when I switch lists and switch back. **Chashe lists locally, then on each read compare the more recent verison(get write dates from them)**
- How do I deal with writing when the user is offline **prompt the user which version they want to open(then overwrite either local or server copy)**
- Should I limit same ip requests/minute **Probably not**


## TODO
- [Create a Link](https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createLink(java.nio.file.Path,%20java.nio.file.Path)) everytime a user wants to allow another user to his list. (Or just update a file with all paths (this would make it really easy to get all users lists))
- setup Date metadata.. local and remote copies should have a Date so you can compare them ANYTIME the user wants to load a list.
- Client get a list of all available lists on the server. OR just local if no internet.
- Client things: fix new list/first list creation. Dont allow switching to another list without saving(or canceling). ListItem creation dies forever on list switch.
- Server, PERMS CLI
- MLOCK things like log


## Day to day:

9/12/2024
- using *rasberry pi imager* software, I can download the ubuntu image for my PI.
- I was able to setup ssh (without keys for now):
- `ufw enable` `ufw allow 22` <- on the server this will enable passage through the firewall and allow connection on port 22 which ssh uses to connect by default
- `ssh myUser@serverDeviceIP` will startup the conection on the client side, though you can also setup the config file within the .ssh folder to make it more streamlined.

9/14/2024
- Lets encrypt uses certbot which you install on your system. This will run a test and give you an SSL certificate

9/15/2024
- Lets encrypt can force all http connections to become https if configured. (HSTS) is a security HTTP header to make sure the connection has the cert, otherwise it will terminate it.
- Certbot will use port 80 to validate the certificate (I belive you must be running apache or something to obtain it)
- Certbot: [Install/Run instructions](https://www.inmotionhosting.com/support/website/ssl/lets-encrypt-ssl-ubuntu-with-certbot/#create). I'm using the snapd install
- I believe I will have to [install Apache](https://ubuntu.com/tutorials/install-and-configure-apache#1-overview) to get the SSL cert. I also think it could be nice and easier to setup other web connections later on if needed

9/17/2024
- Setting up ssh keys. [about](https://www.digitalocean.com/community/tutorials/how-to-configure-ssh-key-based-authentication-on-a-linux-server)
- `ssh-keygen -t ed25519` - from a local machine(not server) I can do this to generate the keys in ed25519 format
- copy `~/.ssh/theKeyFile.pub` to the end of the servers file `~/.ssh/authorized_keys"`. Make sure this file has only 600(r/w) perms
- never save private key on the server, private key will be on the clients that are not open to the internet.
- Disabled password login(use only keys). Within `ssh/sshd_config` AND `ssh/sshd_config.d/50-cloud-init.conf` set `PasswordAuthentication no`. Then run `service ssh restart` to enable the changes
More Today--
- Installed apache. Web pages are in `var/www/` you will need to setup a VirtualHost as well as configurations found in `etc/apache2/`
- Forwarding port 80 will allow the default web page to run - find out more about how to run processes and change url later.

9/19/2024
- Finished apache setup and got the ssl certification using the certbot install instructions listed previously. (it was fairly easy)
- https is on port 443 so I enabled that. dont disable port 80, it will be automatically redirected to https
- Started a neat website structure for fun and eventually create a download page for this project. :: [webserver](https://github.com/HoldenErnest/webserver)
- You can disable certain files and directories from being served by editing `etc/apache2/apache2.conf`. You can look at a list of these [config options](https://httpd.apache.org/docs/current/mod/core.html#directory) for more info. Remeber to `service apache2 restart` for changes to take.

9/20/2024
- Looked into creating ssl sockets for cpp. [sslsocket option](https://github.com/embeddedmz/socket-cpp)

9/22/2024
- Another option may be [LibreSSL](https://www.libressl.org/) which seems like a better fork of OpenSSL, however im unsure if it is C only or CPP compatable

9/23/2024
- Setup ssh keypair for git: `ssh-keygen -t ed25519 -C "your_email@example.com"` found [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent) then copy to github

9/26/2024
- Looked into a Java alternative, it seems like there is more documentation for Javas SSL sockets as seen [here](https://docs.oracle.com/javase/10/security/sample-code-illustrating-secure-socket-connection-client-and-server.htm#JSSEC-GUID-B1060A74-9BAE-40F1-AB2B-C8D83812A4C7)
- [HTTP versions and format](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Evolution_of_HTTP)

6/29/2024
- Learned about javas keystore system, keystores store ssl certs (.pem keys will have to be converted before theyre able to be added). [Java generate self-signed key](https://docs.oracle.com/cd/E19798-01/821-1841/gjrgy/)
- This might be what im looking for: [X509 public cert private key](https://stackoverflow.com/questions/22950226/java-how-to-make-ssl-connection-with-public-certificate-and-private-key)
- package both in the same file (it will ask you to input an export password)(make sure you have read perms): `openssl pkcs12 -export -inkey privkey.pem -in cert.pem -out bothAsPKCS12.p12 -CAfile chain.pem -chain`
- chain certificates allow the certificate to almost be passed along domains, so it would include letsencrypt as a chain
- import that file into a java keystore: `keytool -importkeystore -deststorepass [changeit] -destkeypass [changeit] -destkeystore server.keystore -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass [changeit]`

10/2/2024
- its impossibly simple to create an https server using js.
```
const https = require('node:https');
const fs = require('node:fs');

const options = {
  key: fs.readFileSync('private-key.pem'),
  cert: fs.readFileSync('certificate.pem'),
};

https.createServer(options, (req, res) => {
  res.writeHead(200);
  res.end('hello world\n');
}).listen(8000);
```
- so if needed I can rework the server to be JS as backend(gross). But at least I wouldnt have to worry about the key needing to be converted(for when it needs to be renewed).
- [JS HTTPS client](https://nodejs.org/docs/latest/api/https.html#https_https_get_options_callback)
- To test I will portforward to the server from my PC only and use localhost (so as to not allow security issues while learning about the correct ways to implement it)
- With this I should be able to get a back and forth communication going, it should look like this:
- Created [communicationDiagram.drawio](/communicationDiagram.drawio)

10/3/2024
- Created a Client in JS that connects to the ListServer, the list server also collects this data and the IP connection

10/4/2024
- Created a seperate Header class that parses and stores given headers (this will make it really easy to find out what the client was asking for and what to do with it)
- Compiling all the java files is getting kind of annoying, so instead you can run `find -name "*.java" > compileIt.txt` to get a list of all the java files, then just do `javac @compileIt.txt`.
- At this point I can do a bit with the parsed header options, but I really need to get a user DB and probably a UID folder structure.
- Determining what the best DB to use for Users/Pass and maybe even the files themselves:
- I dont think SQL is good since its massive and you need to run a server for it. [MongoDB](https://www.mongodb.com/) seems like a decent choice.
- [Hashing and Salting](https://auth0.com/blog/adding-salt-to-hashing-a-better-way-to-store-passwords/) could also be a good choice. You want to salt(sprinkle in random characters) before you hash so that no one can reverse-hash easily if they know one of the passwords. YOU SHOULD NOT use the same salt system-wide as anyone can find that and just prepend their searches, instead generate a unique salt for each entry.
- Using json lib, download the jar from [org.json](https://github.com/stleary/JSON-java?tab=readme-ov-file). For now I can compile with: `javac -cp ~/ListServer/lib/json-20240303.jar @compileit` and run with: `java -cp :~/ListServer/lib/json-20240303.jar ClassFileServer 2001`

10/5/2024
- Gave up on Doing anything with json, decided to just store my bytes concatonated (it can still be parsed easily since hash and salt length are static)
- Fully implimented the salting and hashing for users, created a little CLI to add and find users.
- Hashing is irreversible, you can only compare the hash to a new hashed password with the same salt. This is the difference between it and Encryption
### CLI Interaction
```
user add
Enter: <Username> <Password>: fred secretPass
[FILE] Writing to file: /ListServer/root/users/fred
user get
Enter: <Username> <Password?>: fred
User 'fred' found!
user get
Enter: <Username> <Password?>: fred secretPass
User 'fred' found!
user get
Enter: <Username> <Password?>: fred secretPassWhoops
User 'fred' not found with that password.
```
### File contents of users/fred
```
�Ղ�M���vˬ����#�\��?6&���>��`�
```
- Sorted it better though when its in ram. [sort](https://stackoverflow.com/questions/29728756/sorting-objects-in-an-arraylist-based-on-their-string-field), [search](https://stackoverflow.com/questions/12496038/searching-in-a-arraylist-with-custom-objects-for-certain-strings)
- Worked on transmission protocol outline, then sent a login request from client that responded correctly! First relevant transmission.

10/6/2024
- Worked on Reading Lists / Verifying Users / Writing Lists
- Might want to look into [peer certificate checking](https://stackoverflow.com/questions/20624586/is-ssl-enough-for-protecting-a-request-and-its-headers) to add more protection from MitM attacks. (From what I gather this is just client sends key and or cert in the Header).
- Writing got kind of annoying, had to restructure some of the Request since the client `https.get()` doesnt accept body data. Overall I got it working, you can now: `overwrite your own lists, write to your own new lists, and write to others lists with user marked in metafile as write access`.
- TODO: **!IMPORTANT!** make sure users can only create a certain amount of lists / a certain amount of bytes per list.
- 867 total lines :)

10/7/2024
- I figured out a problem: when a list is shared for writing, you might have a problem if multiple people try to access it at the same time.

10/8/2024
- Technically its the 8th, only by a couple hours. I spent time working on being able to send lists to the server on save which its now able to do.
- Server side in.read buffer was only so big, so I had to loop the data to get chunks. (Figured this would eventually be the case)
- I still need to handle exceptions client side however. Failed saves might result in a red notification (detailed error report on hover?)
- I go sleep now
- I worked on getting reading to work. I had to mess around with `async functions`. If a method contains `await` it MUST be async, you dont need async calls to have await unless you care about its return value.
- Reading, Writing, and Login all work pretty well and are implimented into the client pretty much seamlessly
- If reading a list from the server doesnt work it will just grab it from a local copy which it stores with every write attempt. (WARNING) Obviously you cant write to it yet offline since it will currently prioritize the servers copy, maybe I should impliment a date of write with the metadata.
- 876(Server) + 913(Client) = 1789 Total lines
### Login (whats running on the server side)
```
Connection: {User: "bob", Pass: "sss", Mode: "login", List: "", Host: some.hostname.com:2001, Content-Len: 0, Received: 0}
Response 200
Closing connection on a thread
```
### Load List (whats running on the server side)
```
Connection: {User: "bob", Pass: "sss", Mode: "get", List: "newList", Host: some.hostname.com:2001, Content-Len: 0, Received: 0}
[FILE] Loading File: /ListServer/root/lists/bob/newList.csv
Response 200
Closing connection on a thread
```
### Save List (whats running on the server side)
```
Connection: {User: "bob", Pass: "sss", Mode: "save", List: "newList", Host: some.hostname.com:2001, Content-Len: 226, Received: 226}
[FILE] Writing to file: /ListServer/root/lists/bob/newList.csv
Response 200
Closing connection on a thread
```

10/10/2024
- I'm attempting an optimized way to go about versioning (user 1 and 2 open v1: they both make changes, which changes are saved?)
- Using a combination of BaseVersions, ServerVersion, and NewVersion I can accuratly find when there will be conflict errors. In which case I will be able to use `listB.filter(item => !listA.includes(item));` with listB and listA items being each listItem from a list.

10/13/2024
- Past few days I worked on Versioning. Believe its done, should probably make unit tests for it though.
- I still how to work out how I actually want to deal with the conflicts as I mentioned previously, but at least the system is in place for the user to access/eventually compare the two versions.
- I should make a few more modifications to the client list, some QOL features are missing like removing lists
- I should make a CLI for perms, once thats in place I can have users send a Permissions change request, which can allow others read/write access.
- I can put in/take out [hard links](https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createLink(java.nio.file.Path,%20java.nio.file.Path)) when a permission pertaining to a specific user is changed.(this way the users available lists will automatically update)

### Versioning
```
baseVersion is updated on a client WHEN:
  1. a get request is successful, then set it to what the server was.
  2. a save is successful (set to whatever the response version is)
  3. a merge is settled, then set it to what the server was.

Client: save
Server:
  if baseVer != remoteVer respond conflict (300: + send list)
  On conflict.. Client: read list: send to compare / make new list
  On conflict.. Client: save the merged list, baseVer = RemoteVer

Client: get
Server:
  if cachedVer >= remoteVer dont load (cashed list is more recent)

On every 200 get: Client baseVer = remoteVer.
On every 300: compare lists, save resulting merge.
```

10/15/2024
- I decided to deal with conflics by just trashing the local changes, and loading the remote version.
- This is more simplistic and will overall be easier on the user.
- I also worked on getting notifications working. I'm not sure it looks quite right right now but I like the functionality
- I should do either unit testing or extensive manual testing to ensure no problems before release

10/16/2024
- Guess it really is the 16th now.
- Added a CLI for permissions. `Enter: <Username>/<Listname> <Username> <2/1/0(rw/r/-)>: `
- Worked on Hardlinks, they should be in place. Theoretically the hardlinks make it easy and foolproof to use only the lists allowed to you.
- You can technically have a list named `user.listname` but it wont belong to another user, the server doesnt care who it belongs to when its created, if you try to write to anything pre-existing it checks the metadata, which is always copied when you make a hardlink.
- 1144(Server) + 1055(Client) = 2199(Total)

10/26/2024
- I've been fixing a few client things in between now and then.
- Added notifications, fixed multiple client bugs (read the commits/todo for my List repo)
- potential optimization on server, if the user doesnt have the correct Username and Password, IMMEDIATLY close the connection. This way we dont waste cpu time reading whatever data they mightve sent with the request.
- refactored client windows, added settings window option
- lists load from cached version first(ALWAYS), if its ever loaded from the server it means you were out of date, in which a notification is pushed to you

11/4/2024
- I've continued working on client side settings for a bit. Its pretty much done, just want to polish some things up before release still
- I'm testing making a build for linux since my user is switching platforms. Doing the things mentioned [here](https://www.electronforge.io/guides/developing-with-wsl) I can get npm running on it. (I also tried [this versioning tool](https://github.com/nvm-sh/nvm) but im unsure if thats what made it work)
- check [these](https://www.electronjs.org/docs/latest/development/build-instructions-linux/) build instructions as well
- NVM, its done.. To install the app on a linux machine, use `sudo dpkg --install <lupuvault installer>.deb`