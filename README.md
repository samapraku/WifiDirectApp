This app uses WiFi Direct (P2P) to communicate with Raspberry pi 3 model B.

It is a derivation based on the following works:  
https://github.com/martinohanlon/BlueDot  
https://github.com/luckyhandler/example-wifi-direct

The Raspberry pi app counterpart was developed using Django.  
https://github.com/samapraku/Django-2D-Breakout

WiFi Direct works at the network level. Once a connection is established, communication takes place via tcp sockkets.  
The Django application should run on the same subnet or network as the smartphone is connected to.
The android app connects on port **8888**

The port can be changed in this file:  
[WifiP2pConnectionManager.kt#L63](https://github.com/samapraku/WifiDirectApp/blob/master/app/src/main/java/com/ansere/mobile/wifidirect/WifiP2pConnectionManager.kt#L63)
