Mule AS2 Transport - User Guide 
===============================

Welcome to AS2
--------------

The Applicability Statement 2 or AS2 specification defines a mechanism for the secure and reliable transfer of structured
business data over the internet.
Some of the key benefits of using AS2 includes:

- File are encoded as "attachments" in a standardized S/MIME message (AS2 message)
- Messages can be signed, but do not have to be
- Messages can be encrypted, but do not have to be
- Messages may request a synchronous or asynchronous Message Disposition Notification (MDN) back, but do not have to request such a message


The MDN does not imply that a received business document has been processed without errors by the receiving trading partner. An MDN only 
confirms that the message transmission completed successfully, and has now been received by the AS2 infrastructure of the receiving trading partner.

At the moment the only version of the protocol supported is un-encrypted messages with signature and a synchronous MDN not signed.


Core Transport Principles
-------------------------

Here is a quick review of the behaviour of the main components

- The connector element manages a set of common properties that will be shared by all endpoints, in particular the informations concerning the keystore
  and the partner identifier.
- The inbound endpoint receives a POST from an AS2 Client and it makes sure the AS2 message is well formed and the signature is verified.
  After that it sends back a MDN accordingly with the verifications.
- The outbound endpoint receives a mule message, take the payload and uses it to build the AS2 message to send. The AS2 message is an S/MIME message, 
  the first part contains un-encrypted data and the second the signature. It sends the message in a POST to the AS2 partner and it waits to receive the 
  synchronous MDN. 

### Message Payload 

Configuration Reference
-----------------------

All the configuration parameters supported by the connector and endpoint configuration elements are described in this section.

### Connector Attributes

The AS2 connector is based on HTTP connector and supports all its attributes together with AS2 specific parameters.

<table class="confluenceTable" >
	<tr>
		<th>Name</th>
		<th>Type</th>
		<th>Required</th>
		<th>Default</th>		
		<th>Description</th>
	</tr>
	<tr>
		<td>senderId</td>
		<td>string</td>
		<td>yes</td>	
		<td></td>
		<td><p>The Sender Identifier. The name the business partner is expecting in the AS2 Message for the header "AS2-from"</p></td>							
	</tr>
	<tr>
		<td>keytorePath</td>
		<td>string</td>
		<td>yes</td>	
		<td></td>
		<td><p>Path where to find the keystore used to store the Private Key and partner's Certificate</p></td>						
	</tr>
	<tr>
		<td>keytorePassword</td>
		<td>string</td>
		<td>yes</td>	
		<td></td>
		<td><p>Password to open the Keystore</p></td>							
	</tr>	
	<tr>
		<td>partnerId</td>
		<td>string</td>
		<td>yes</td>	
		<td></td>
		<td><p>Business Partner Identifier, it specifies the receiver of the message. The name the business partner is expecting in the AS2 Message for the header "AS2-to"</p>
		</td>		
	<tr>
		<td>partnerId</td>
		<td>string</td>
		<td>yes</td>	
		<td></td>
		<td><p>Business Partner Identifier, it specifies the receiver of the message. The name the business partner is expecting in the AS2 Message for the header "AS2-to"</p></td>							
	</tr>				
</table>

This connector also accepts all the attributes from HTTP Connector

### Inbound Endpoint Attributes

An inbound AS2 endpoint has no different attributes from an HTTP inbound endpoint, but an important one is exchange-pattern.
You can check the available ones from the <a href="http://www.mulesoft.org/documentation-3.2/display/MULE2USER/HTTP+Transport" title="HTTP Transport">HTTP inbound endpoint</a>.

<table class="confluenceTable" >
	<tr>
		<th>Name</th>
		<th>Type</th>
		<th>Required</th>
		<th>Default</th>		
		<th>Description</th>
	</tr>
	<tr>
		<td>exchange-pattern</td>
		<td>one-way, request-response</td>
		<td>no</td>	
		<td>one-way</td>
		<td><p>It defines if the endpoint sends the MDN immediately back to sender, one-way, or it waits for the completion of the flow, request-response; in the second case it sends back a MDN UNEXPECTED_PROCESSING_ERROR if an exception occurs during the flow</p></td>							
	</tr>
</table>

For example:

	<as2:inbound-endpoint address="as2://localhost:8081/as2-receiver" exchange-pattern="request-response" />


### Outbound Endpoint Attributes

<table class="confluenceTable" >
	<tr>
		<th>Name</th>
		<th>Type</th>
		<th>Required</th>
		<th>Default</th>
		<th>Description</th>								
	</tr>
	<tr>
		<td>fileName</td>
		<td>string</td>
		<td>yes</td>
		<td></td>
		<td>The file name to use in the AS2 message for the attached file</td>
	</tr>
	<tr>
		<td>subject</td>
		<td>string</td>
		<td>no</td>
		<td></td>
		<td>The subject header used in the AS2 message</td>
	</tr>
</table>




For example:

	<as2:outbound-endpoint address="as2://localhost:8080/as2/as2-receiver" fileName="prefix_name_#[function:datestamp:yyMMdd.HHmmss.SSS].dat subject="sku" />"



### Transformers

There is just one transformer for this transport. Note that this is added automatically to the Mule registry at start up.


<table class="confluenceTable" >
	<tr>
		<th>Name</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>as2object-to-http-request-transformer</td>
		<td>The transformer will create a valid HTTP request using current message and setting all the AS2 headers</td>
	</tr>
</table>
