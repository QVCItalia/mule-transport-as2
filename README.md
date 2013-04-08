Mule AS2 Transport
==================

The AS2 (Applicability Statement 2) Transport allows to send files to an AS2 server and receive file from an AS2 client.
The AS2 Protocol is specified in the RFC-4130 (http://www.ietf.org/rfc/rfc4130.txt).

Read the [complete user guide](https://github.com/QVCItalia/mule-transport-as2/blob/master/GUIDE.md).

Supported AS2 Versions
----------------------

At the moment the transport supports:

- Inbound endpoint receives un-encrypted data with a signature and returns a synchronous receipt (MDN) not signed
- Outbound endoint sends un-encrypted data with a signature and waits for a synchronous receipt (MDN) not signed

Maven Support
-------------

To add the Mule AS2 transport to Maven project add the following dependency:

	<dependency>
		<groupId>org.mule.transports</groupId>
		<artifactId>mule-transport-as2</artifactId>
		<version>x.y.z</version>
	</dependency> 