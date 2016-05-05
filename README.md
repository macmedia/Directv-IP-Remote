# ![directv-logo](https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/DIRECTV.png) Directv-IP-Tuner
Veraion 1.0.0 - 05/05/2016

## This is not a _stable_ list of files. Im using this a staging area for files that Im working on.
---
* **/Proof of concept** - is a groovy (Smartthings) file that Im working on that will auto discover
DirectTV IP based set top boxes.
* **/Icons** - just a logo to use with the Smartthings App
* **/devicetypes** - Smartthings device type groovy file
* **/smartapps** - Smartthings Connect and child app

---
## Description
Im creating this app so I can automatically create virtual device buttons that can be used with Amazon Echo to change the stations on the directv box. The idea is you can add a button that is called TV NBC and when telling Alexa to "turn on TV NBC" the station will change to NBC.

After all your stations have been added you will need to give access to the newly created buttons in the Alexa smart app and then issue a "Discover new devices" command to Alexa

Mapping and name "NBC" to a number 12. Then when you say, "Alexa, turn on TV NBC", it will send the command to the Directv ip/port to change the channel.


<img src="https://raw.githubusercontent.com/macmedia/Directv-IP-Tuner/master/Icons/IMG_0141.png" width="300px">
