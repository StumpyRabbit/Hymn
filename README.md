# Raven - basically working in the 2-vat test cases. BROKEN due to lack of STON encoding in Java, which I am working to implement.

9-layer decentralized protocol cloudstack talking ParrotTalk

The current use of ParrotTalk is with the Raven system, a promise-based distributed object implementation. I am working to bring ParrotTalk to Java and allow Raven to operate interdependently between Squeak, Pharo, Java and any other languages which can support ParrotTalk and STON. My latest efforts with Raven are to bring STON as the Layer 6 encoding. 

http://www.squeaksource.com/Oceanside/Ston-Core-SvenVanCaekenberghe.36.mcz

http://www.squeaksource.com/Oceanside/STON-Text%20support-TheIntegrator.2.mcz

http://www.squeaksource.com/Oceanside/Ston-Tests-SvenVanCaekenberghe.34.mcz

http://www.squeaksource.com/Cryptography/Raven-rww.24.mcz

Here is a log of the 2-vat test in Java:

https://gist.github.com/RobWithers/2b428ff541bfdc9d85699c8c1729f34c

Here is a diagram of the protocol stack

![Protocol Stack](https://github.com/CallistoHouseLtd/ParrotTalk/blob/master/docs/a%20Transceiver.jpeg)
