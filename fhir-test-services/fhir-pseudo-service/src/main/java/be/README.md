## Introduction

The goal of this document is to describe the VAS transversal library for the pseudonymisation and how to integrate it in your implementation to use the different methods available in the pseudonymisation service (see [pseudonymisation cookbook](https://www.ehealth.fgov.be/ehealthplatform/fr/service-codage-anonymisation-et-ttp#docType4) on the eHealth plateform).
It will also be useful to understand the different concepts of the pseudonymisation.

This document is intended to be useful to everyone, whether you are in a functional or technical role.


> ⚠️ important note: this “Library” cannot be used as is. The unique objective is to share the source code in order to give an overview of the complexity of the code and to be able to draw inspiration from it in the development of your own component based on eHealth "Blinded Pseudonymization" Service technical documentation.
This library was designed for machine to machine needs. An adaptation is necessary in order to use the token received when connecting as a physical person (HealthCare Client Registration).


## Definition of functional terms used in this page

<details>
  <summary>Click to view the definitions</summary>

### Identifier

An identifier is a unique code associated to a single citizen, which identifies a citizen without requiring additional information.
The most common identifier in Belgium is the social security number (rijksregisternummer/numéro de registre national)

### Pseudonym (at rest)

A pseudonym is a unique code associated to one citizen. Unlike identifiers, pseudonyms are associated with a specific domain (see below). Hence, pseudonyms have only a local significance. It means that this pseudonym does not allow someone to know who is the real citizen behind without additional information. Medical data can be linked together thanks to this local pseudonym but it does not suffice to link those data to the real person outside of this domain. A pseudonym enables linkage of records belonging to the same citizen, without the need of identifying the real citizen behind.

### Pseudonym in transit for domain A

Pseudonym in transit is a pseudonym with extra protection layers that are only present during transit.

### Domain

A domain is pseudonymized data set, i.e. a domain consists of multiple records of the form (pseudonym, data). Each pseudonym is associated with a single domain and has only meaning within this domain. A domain could be a specific backend database or a pseudonymized dataset required for a specific research project. Domains should never overlap. Hence, domains can be thought of as separate islands; on each island, the citizen is only known by his/her island-specific pseudonym.

### Blinding

A content is "blinded" if this content cannot be found/seen by another application. It can be seen like an encryption with a single-use key.
Each time that a content is "blinded", a new key is used.

</details>

## Real use case example

This section aims to explain the use of the pseudonymisation in a real use case.
This section will focus on the manipulation of the patient identifier from a security point of view and will not focus of on the format of this identifier. It means that it will not describe the conversion needed to have the input in the correct format (EcPoints, ...) but it will give the information of protection layer that will be applied to ensure the privacy of the patient (blinding, encryption, ...).

> #### Important information
> Those protection layers are automatically applied by the library when a method is called. It will then not be present in the method explanation.\
> The conversion needed to call the methods available will be explained further down in this document.

In this example, a prescriber will consult prescriptions for a patient thanks to an integrator software. The back-end (called UHMEP in our example) will call an external eHealth service (Attribute Authority) to verify the therapeutic relations (therapeutic link + exclusion) to see if the prescriptions can be returned.
When this check is done, the prescriptions will be returned to the prescriber.

(An explanatory text of the diagram is present just after it.)
![Sequence diagram - Consult prescriptions for a patient](<Consult prescriptions for a patient.png>)

The patient identifier may not be given to the back-end application. The integrator software will then use the library to pseudonymize this identifier which is the Belgian Social Security Identification Number (SSIN) to a pseudonym for the domain of the targeted back-end (UHMEP in this case).

The library will blind this identifier and call the "pseudonymize" method of the eHealth pseudonymisation service. The response will be unblinded and returned to the integrator.
The blinding is necessary at each call to the eHealth pseudonymisation service and avoid this service to see any kind of identifier for the patient (SSIN, pseudonym in transit, pseudonym at rest).

The pseudonym in transit returned by the library can be used to ask prescriptions to the back-end.
This pseudonym in transit will be decrypted by the back-end to remove the protection layer which is present during data exchange.

The result of this decryption is called the pseudonym at rest which will be stored in the back-end database. This pseudonym at rest will always be the same if the patient identifier is the same. Which is not the case for the pseudonym in transit, it will be always different.

This pseudonym at rest can be used to retrieve the prescriptions of the patient but first, the back-end needs to verify if those prescriptions can be consulted by the prescriber by checking the therapeutic relations.

For doing this, the back-end must call an external eHealth service and communicate to this service for which patient the verification must be done. This will be done by giving a pseudonym in transit for the domain "eHealth".

To obtain this pseudonym, the back-end can use the library to convert his pseudonym at rest to a pseudonym in transit eHealth.

Once the verification is done, the prescriptions can be returned by the back-end with a new pseudonym in transit for the patient information (by encrypting the pseudonym at rest).

The integrator software can use the library to identify this pseudonym in transit to the starting identifier.
In this case, this last call is not necessary but will be if the prescriber consult prescriptions that he created and then, receives prescriptions for many patients.

## VAS library dependencies

### VAS library dependencies 0.6.2

```xml
<javaVersion>17</javaVersion>
 
<!-- Cryptography libraries -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.72</version>
</dependency>
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.31</version>
</dependency>
     
<!-- Logging facade -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
</dependency>
```

## Initialization of the VAS pseudonymisation library in a native way

The PseudonymisationHelper is initialized using a constructor which requires specific parameters to be provided.
This guide outlines the steps and parameters needed for a successful initialization.

### Required parameters

Ensure that you have the required parameters for initialization:

- **domainKey** : *String* | Your domain key provided by eHealth
- **additionalDomains** :  *Set\<String>* | A Set of domain keys you will deal with
- **jwksUrl** : *URI* | The JWKS uri of your domain
- **pseudonymisationClient** : *Interface* | A REST client to communicate with eHealth.[^1]
- **jwksClient** : *Interface* | A client who consumes the JWKS.[^1]
- **privateKeySupplier** : *Interface* | A PrivateKey supplier that will handle Keystore and secrets.[^1]

[^1]: Standard implementation is provided but a custom implementation can be build.

### Code snippets

#### StandardPrivateKeySupplier

```java
final var keystore = new File("path of your keystore");
final var keystorePassword = "Your keystore password";
final var keystoreSupplier = new StandardKeystoreSupplier(keystore, keystorePassword);
final var passwordSupplier = new StandardKeystoreEntryPasswordProtectionSupplier(keystorePassword);
 
final var privateKeySupplier = StandardPrivateKeySupplier(keystoreSupplier, passwordSupplier);
```

#### StandardPseudonymisationClient

```java
final var restClient = StandardPseudonymisationClient.builder()
                .clientId("Your client id")
                .realmUrl("https://api-acpt.ehealth.fgov.be/auth/realms/M2M")
                .pseudoUrl("https://api-acpt.ehealth.fgov.be/pseudo/v1")
                .authenticationAlias("Your authentication alias or 'authentication' if blank")
                .privateKeySupplier(privateKeySupplierImplementation)
                .fromHeaderValue("uhmep@smals.be")
                .build();
```

1. When the StandardPseudonymisationClient is used, your user agent will be automatically defined as "vas-integrations-pseudonymisation/X.Y.Z" where X.Y.Z is the version of the library

2. If you implement your own eHealth REST client, the USER-AGENT and FROM header value specified in the eHealth cookbook must be respected.

#### PseudonymizationHelper initialization example

```java
final var domainKey = "uhmep_v1";
final var foreignDomain = Set.of("ehealth_v1", "seclog_v1");
final var jku = new URI("https://api-acpt.ehealth.fgov.be/etee/v1/pubKeys/cacerts/jwks?identifier=0406798006&type=CBE&applicationIdentifier=UHMEP&use=enc");
final var keystore = new File("/keystorepath.p12");
final var keystorePassword = "secret";
final var privateKeySupplier = // See code snippets StandardPrivateKeySupplier
final var restClient = //  See code snippets StandardpseudonymisationClient
                
final var helper = new PseudonymisationHelper(
        domainKey,
        foreignDomain,
        jku,
        restClient,
        new StandardJwksClient(),
        privateKeySupplier);
```

## Explanation of the VAS library

The VAS pseudonymisation library operates as a set of factories.

### Get a Domain

The first step after the initialization for using the VAS library is to recover a domain, it will give a [Domain](#domain) object that contains a set of factories that helps you to create different objects useful for the pseudonymisation.

The PseudonymisationHelper class provides two methods for obtaining a domain.

```java
// Return the domain specified in the PseudonymisationHelper constructor using the domainKey
Domain getDomain();
 
// Return the domain specified only if the domainKey has been added in additionalDomains in the PseudonymisationHelper constructor, else IllegalStateException is raised.
Domain getDomain(String domainKey);
```

### ValueFactory

The ValueFactory allows the creation of [Value](#value) object, such as a clear identifier to be pseudonymised or a pseudonym identified.
It serves as the entry point to pseudonymize an identifier or the output when a pseudonym is identified.

#### Public Method

```java
// Return the maximum byte size for an identifier.
int getMaxValueSize();
```

The value is calculated as CurveFieldSize / 8 - Buffer size from the domain - 1.\
If the byte size of your identifier exceeds this value, an InvalidValueException is raised.

#### Public Factory

```java
// Create a Value object from a byte array.
Value from(byte[] var1) throws InvalidValueException;
 
// Create a Value object from a string using a specified charset.
Value from(String var1, Charset var2) throws InvalidValueException;
 
// Create a Value object from a string using the UTF-8 charset.
Value from(String var1) throws InvalidValueException;
```

### PseudonymFactory

The PseudonymFactory is responsible for creating [Pseudonym](#pseudonym) object from points on the curve  X and Y.\
If the point is invalid then InvalidPseudonymException is raised.

```java
// Create a Pseudonym object from BigIntegers representing X and Y coordinates.
Pseudonym fromXY(BigInteger var1, BigInteger var2) throws InvalidPseudonymException;
 
// Create a Pseudonym object from byte arrays representing X and Y coordinates.
Pseudonym fromXY(byte[] var1, byte[] var2) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X and Y coordinates encoded as BigIntegers in Base64 format using a specified Base64 decoder.
Pseudonym fromXY(String var1, String var2, Base64.Decoder var3) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X and Y coordinates encoded as BigIntegers in Base64 format using the default java.util.Base64 decoder.
Pseudonym fromXY(String var1, String var2) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X coordinates represented in ASN.1 format.
Pseudonym fromAsn1(byte[] var1) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X coordinates encoded in ASN.1 format using a specified Base64 decoder.
Pseudonym fromAsn1(String var1, Base64.Decoder var2) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X coordinates encoded in ASN.1 format using the default java.util.Base64 decoder.
Pseudonym fromAsn1(String var1) throws InvalidPseudonymException;
 
// Create a Pseudonym object from X and Y coordinates encoded as Base64, separated by a specified character (DotFormat)
Pseudonym fromXAndYAsBase64SeparatedBy(String var1, char separator) throws InvalidPseudonymException;
```

### TransitInfoFactory

The TransitInfoFactory facilitates the creation of [TransitInfo](#transitinfo) objects from a JWECompact representation.

#### Public Factory

```java
// Create a TransitInfo object from a JWECompact representation.
TransitInfo from(final String jweCompact);
```

The parsing of the JWECompact is performed using the NimbusDS library nimbus-jose-jwt version 9.31.\
If the input is an invalid JWECompact then an InvalidTransitInfoException is raised.

### PseudonymInTransitFactory

This factory allows to create a [PseudonymInTransit](#pseudonymintransit) object from a pseudonym and a transitInfo.

#### Public Factory

```java
// Create PseudonymInTransit from a Pseudonym and TransitInfo objects.
PseudonymInTransit from(final Pseudonym pseudonym, final TransitInfo transitInfo);
 
// Create PseudonymInTransit from a String asn1:transitInfo where asn1 is the pseudonym ASN1 base64 and TransitInfo is JWECompact
PseudonymInTransit fromAsn1AndTransitInfo(final String asn1AndTransitInfo) throws InvalidPseudonymException;
```

### PseudonymAtRestFactory

This factory allows to create a [PseudonymAtRest](#pseudonymatrest) from ASN1 point representation.

#### Public Factory

```java
// Create a PseudonymAtRest from ASN1 binary representation, raise InvalidPseudonymException if coordinate is invalid
PseudonymAtRest fromAsn1(final byte[] asn1) throws InvalidPseudonymException;

// Create a PseudonymAtRest from ASN1 Base64 encoded using a specified Decoder, raise InvalidPseudonymException if coordinate is invalid
PseudonymAtRest fromAsn1(final String asn1, final Base64.Decoder base64Decoder) throws InvalidPseudonymException;

// Create a PseudonymAtRest from ASN1 Base64 encoded using java.util.Base64 decoder, raise InvalidPseudonymException if coordinate is invalid
PseudonymAtRest fromAsn1(final String asn1) throws InvalidPseudonymException;
```

### VAS pseudonymisation library objects

#### Domain

The Domain object represents your or a foreign Domain. The object contains methods to access to the factory.

##### Public methods of Domain object

```java
ValueFactory valueFactory();

PseudonymFactory pseudonymFactory();

PseudonymInTransitFactory pseudonymInTransitFactory();

PseudonymAtRestFactory pseudonymAtRestFactory();

TransitInfoFactory transitInfoFactory();
```

#### Value

A  Value object represents a clear identifier, this object allows to pseudonymize an identifier.

##### Public methods of Value object

```java
// Return the identifier as byte arrays
byte[] asBytes();

// Return the identifier as String with a specified charset
String asString(final Charset charset);

// Return the identifier as String using default UTF_8
String asString();

// Pseudonymize the Identifier and return a PseudonymInTransit
PseudonymInTransit pseudonymize();
```

#### Pseudonym

A Pseudonym object represents a point X and Y in a curve.

##### Public methods of Pseudonym object

```java
// Return the X as BigInteger
BigInteger x();

// Return the Y as BigInteger
BigInteger y();

// Return the X as byte arrays
byte[] xAsBytes();

// Return the Y as  byte arrays
byte[] yAsBytes();

// Return the X as byte arrays encoded in Base64 with Base64 encoder specified
String xAsBase64(final Encoder base64Encoder);

// Return the X as byte arrays encoded in Base64 using java.util.base64
String xAsBase64();

// Return the Y as byte arrays encoded in Base64 with Base64 encoder is specified  
String yAsBase64(final Encoder base64Encoder);

// Return the Y as byte arrays encoded in Base64 using java.util.base64   
String yAsBase64();

// Return the DotFormat
String xAndYAsBase64SeparatedBy(final String separator);

// Return the representation as ASN1
byte[] asn1();

// Return the representation as ASN1 with Base64 encoder specified
String asn1Base64(final Encoder base64Encoder);

// Return the representation as ASN1 using Base64 java.util.base64
String asn1Base64();

// Return the representation as ASN1 compressed
byte[] asn1Compressed();

// Return the representation as ASN1 compressed with Base64 encoder specified
String asn1Base64Compressed(final Encoder base64Encoder);

// Return the representation as ASN1 using Base64 java.util.base64
String asn1Base64Compressed();
```

#### TransitInfo

A TransitInfo object represents the JWECompact transitInfo.

##### Public methods of TransitInfo object

```java
// Return the JWECompact as string serialized
String asString();

// Return the audience name from the TransitInfo
String audience() throws InvalidTransitInfoException;

// Check if the audience in TransitInfo match with domain audience, else raise InvalidTransitInfoException
void validateHeader() throws InvalidTransitInfoException;
```

#### PseudonymInTransit

A pseudonymInTransit object represents a pseudonymInTransit that contains a Pseudonym and TransitInfo objects.

##### Public methods of TransitInfo object

```java
// Return the TransitInfo of PseudonymInTransit
TransitInfo transitInfo();

// Return the issue at
Long iat();

// Return the expiration time
Long exp();

// Return the asn1 compressed as asn1:transitinfo in base64
String asString();

// Try to identify the pseudonymInTransit and return Value object
Value identify();

// Try to decrypt the pseudonymInTransit and return PseudonymAtRest
PseudonymAtRest decrypt() throws InvalidTransitInfoException;

// Check if pseudonymInTransit is not expired then decrypt and return PseudonymAtRest
PseudonymAtRest decrypt(boolean validateIatAndExp) throws InvalidTransitInfoException;

// Convert the pseudonymInTransit to a pseudonymInTransit of another domain
PseudonymInTransit convertTo(String toDomainKey);
```

#### PseudonymAtRest

A PseudonymAtRest object represents a PseudonymAtRest.

##### Public methods of PseudonymAtRest object

```java
// Convert the pseudonymAtRest to a PseudonymInTransit
PseudonymInTransit convertTo(final String toDomainKey);

// Create a pseudonymInTransit from pseudonymAtRest
PseudonymInTransit createPseudonymInTransit();
```

## Use of the VAS library in real use cases

These examples demonstrate how to use the VAS library for mainly use cases.

### Pseudonymize an identifier

The goal of this use case is to create a pseudonym for a specific domain from a patient identifier.
The output will be a pseudonym in transit which can be decrypted only by the domain owner.

#### Code snippets

```java
final Pseudonym pseudonym = pseudonymisationHelper.getDomain()
                .valueFactory()
                .from("your identifier")
                .pseudonymize();
```

### Identify a pseudonym in transit

The goal of this use case is to identify a pseudonymInTransit to a clear identifier (inverse of pseudonymize operation).

#### Code snippets

```java
final Value identifier = pseudonymisationHelper.getDomain()
.pseudonymInTransitFactory()
.from(pseudonym, transitInfo)
.identify();
```

To see how to obtain pseudo and transitInfo objects, refer to [PseudonymFactory](#pseudonymfactory) & [TransitInfoFactory](#transitinfofactory)

### Decrypt a pseudonym in transit

The goal of this use case is to decrypt a pseudonymInTransit to get a pseudonymAtRest.

#### Code snippets

```java
final PseudonymAtRest pseudonymAtRest = pseudonymisationHelper.getDomain()
.pseudonymInTransitFactory()
.from(pseudonym, transitInfo)
.decrypt();
```

To see how to obtain pseudo and transitInfo objects, refer to [PseudonymFactory](#pseudonymfactory) & [TransitInfoFactory](#transitinfofactory)

### Convert a pseudonym AtRest

The goal of this use case is to convert a pseudonymAtRest to a pseudonymInTransit for another domain.

#### Code snippets

```java
final PseudonymInTransit converted = pseudonymisationHelper.getDomain()
.pseudonymAtRestFactory()
.fromAsn1(pseudoAtRest)
.convertTo(target);
```

To see how to obtain pseudoAtRest ASN1, refer to [PseudonymAtRestFactory](#pseudonymatrestfactory) & [convert dotFormat to ASN1](#from-pseudonymatrest-dotformat-to-pseudonymatrest-object)

### Convert a pseudonym in transit

The goal of this use case is to convert a pseudonymInTransit for one domain "A" to a pseudonymInTransit for another domain "B".

> #### Important information
> Pay attention that in this case the domain of the pseudonym in transit to be converted must be specified (domain A).

#### Code snippets

```java
final PseudonymInTransit converted = pseudonymisationHelper.getDomain("domain_A")
.pseudonymInTransitFactory()
.from(pseudo,transitInfo)
.convertTo("domain_B");
```

To see how to obtain pseudo and transitInfo objects, refer to [PseudonymFactory](#pseudonymfactory) & [TransitInfoFactory](#transitinfofactory)

## How to store pseudonyms ?

This section details the procedure for storing pseudonymAtRest focusing on two methods: [ASN.1](#asn1) and [DotFormat](#dotformat).

### ASN.1

ASN.1 is the standard for pseudonym storage.

#### Data Representation

- Only X is stored as binary data.
- The pseudonym library is designed to retrieve the Y value independently.

For more information on ASN.1, refer to [Wikipedia - ASN.1](https://fr.wikipedia.org/wiki/ASN.1).

### DotFormat

> #### Deprecated
> This method is now deprecated.

The DotFormat is the initial method employed for pseudonym storage.
The form of two BigInteger values, X and Y, obtained when a pseudonymInTransit is decrypted.

#### Data Representation

- X and Y are encoded in base64.
- Both values are separated by a dot.
- The combined string is stored as a single string in the database.

#### DotFormat example

`AaGfNpk0vXEp8/ifJLtCwrQ9dfVkfdaccZEyxb2AmOis.AXqnxtrHmuUEOQxUTL8SjchqkNPpHQfqWfBVCwThFBJK6`

## Extra information

### From Encoded Base64 to PseudonymInTransit

In UHMEP, we communicate pseudonyminInTransit with stackholders using the eHealth json response encoded in Base64Url.

For instance a pseudonymInTransit from eHealth:

```json
{
"id": "70a1eedc-d2a4-4b5d-b264-8c4be3e4c06d",
"domain": "uhmep_v1",
"crv": "P-521",
"iat": 1677231615,
"exp": 1677232215,
"x": "IYzzUb0l5mmewxHJ+6i3qijK",
"y": "ATSq992VI1ojO0fEbUNiO08d",
"transitInfo": "eyJhbGciOiJkaXI",
"inResponseTo": "989580dd-c17e-4c57-bab4-0cec27b7a859"
}
```

Will be in Base64URL encoded:

`eyAKICAiaWQiOiAiNzBhMWVlZGMtZDJhNC00YjVkLWIyNjQtOGM0YmUzZTRjMDZkIiwKICAiZG9tYWluIjogInVobWVwX3YxIiwKICAiY3J2IjogIlAtNTIxIiwKICAiaWF0IjogMTY3NzIzMTYxNSwKICAiZXhwIjogMTY3NzIzMjIxNSwKICAieCI6ICJJWXp6VWIwbDVtbWV3eEhKKzZpM3FpaksiLCAKICAieSI6ICJBVFNxOTkyVkkxb2pPMGZFYlVOaU8wOGQiLCAKICAidHJhbnNpdEluZm8iOiAiZXlKaGJHY2lPaUprYVhJIiwKICAiaW5SZXNwb25zZVRvIjogIjk4OTU4MGRkLWMxN2UtNGM1Ny1iYWI0LTBjZWMyN2I3YTg1OSIgCn0`

When we receive a pseudonymInTransit, the first step will be to deserialize the base64URL to a DTO or a MAP<String,Object> that we can get values and pass through the VAS library.

#### Code snippets

```java
final var encoded = "eyAKICAiaWQiOiAiNzBhMWVl...";
final var decoded = Base64.getUrlDecoder().decode(encoded);
final var dto = objectMapper.readValue(decoded, PseudoInTransitDto.class);

// Create a pseudonym using X and Y from dto
final var pseudonym = domain.pseudonymFactory()
.fromXY(dto.x(), dto.y());

// Create a transitInfo from dto
final var transitInfo = domain.transitInfoFactory()
.from(dto.transitInfo());

// Create pseudonym InTransit from Pseudonym and TransitInfo created previously
final var pseudonymInTransit = domain.pseudonymInTransitFactory()
.from(pseudonym, transitInfo);
```

Now you have a pseudonymInTransit object that you can use.

### From PseudonymInTransit to Base64URL

In the same way as the previous section, UHMEP returns pseudonymInTransit encoded in Base64Url, here is an example how to

#### Code snippets

```java
final var pseudoInTransit = pseudonymisationHelper.getDomain()
                .pseudonymAtRestFactory()
                .fromAsn1(pseudonym)
                .createPseudonymInTransit();
final var dto = PseudoInTransitDto.builder()
            .x(pseudoInTransit.xAsBase64())
            .y(pseudoInTransit.yAsBase64())
            .transitInfo(pseudoInTransit.transitInfo().asString())
            .exp(pseudoInTransit.exp())
            .iat(pseudoInTransit.iat())
            .build();
final var writer = objectMapper.writer();
final var json = writer.writeValueAsString(dto);
final var encoded = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
```

### From pseudonymAtRest dotFormat to pseudonymAtRest object

PseudonymAtRestFactory only allows to create a pseudonymAtRest by ASN1, if you use the dotFomat you will need to convert the dotFormat to a Pseudonym object and convert it to ASN1.

#### Code snippets

```java
final var dotFormat = "xxxxx.yyyyy";
final var pseudonymAsn1 = domain.pseudonymFactory()
                .fromXAndYAsBase64SeparatedBy(dotFormat, '.')
                .asn1();

final var pseudoAtRest = pseudonymisationHelper.getDomain()
                .pseudonymAtRestFactory()
                .fromAsn1(pseudonymAsn1)
```

## References

- Generic pseudonymization service - Formal description of the basic concepts (nymservice-formal.docx)
    - Author : Kristof Verslype, Smals Research
