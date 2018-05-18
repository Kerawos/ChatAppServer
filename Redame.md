#Chat Application 2017

#Technologies:
Spring Boot, Spring MVC, JavaFX

#How To
 1. Download Java JRE and install it
 2. Download Server: https://github.com/Kerawos/ChatAppServer/blob/master/download/SerwerChat.jar
 3. Download Client: https://github.com/Kerawos/ChatAppClientFX/blob/master/Download/ChatApp.jar
 4. Run Server first: 'ServerChar.jar'
 5. Run Client: 'ChatAppClientFX.jar' 
 6. To connect from Other PC you have to make sure your firewall is off
 
#Run Client from other computer
You can run clients as many as possible, do not forget to have unique nick for every of them.
At this moment if you wanna connect your Client from other IP then you have to configure Client:
ChatAppClientFX\src\main\java\pl\mareksowa\models\ChatSocket.java : [localhostURL]
Server stand locally on localhost so if your colleagues wanna chat with you so they have to configure client -> to proper IP address

