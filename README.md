# Socket Server/Client Connection
To establish a tcp connection between the host and the android device, adb forwarding is needed. After the connection is established, a client thread is used to monitor reading from the server socket on the android device. 
# Continuous input
After the socket connection is established, echoing is implemented through usage of Scanner.
