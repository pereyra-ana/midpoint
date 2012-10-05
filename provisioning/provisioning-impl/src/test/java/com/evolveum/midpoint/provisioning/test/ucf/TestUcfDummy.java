/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner] Portions Copyrighted 2011
 * Peter Prochazka
 */
package com.evolveum.midpoint.provisioning.test.ucf;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.UcfException;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * 
 * @author Radovan Semancik
 * 
 * This is an UCF test. It shold not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 */
@ContextConfiguration(locations = { 
		"classpath:application-context-provisioning-test.xml",
        "classpath:application-context-audit.xml",
		"classpath:application-context-configuration-test-no-repo.xml" })
public class TestUcfDummy extends AbstractTestNGSpringContextTests {

	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/object/resource-dummy.xml";
	private static final String FILENAME_CONNECTOR_DUMMY = "src/test/resources/ucf/connector-dummy.xml";
	private static final String ACCOUNT_JACK_USERNAME = "jack";
	private static final String ACCOUNT_JACK_FULLNAME = "Jack Sparrow";

	private ConnectorFactory manager;
	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	private ConnectorType connectorType;
	private ConnectorInstance cc;
	private ResourceSchema resourceSchema;
	private static DummyResource dummyResource;

	@Autowired(required = true)
	private ConnectorFactory connectorFactoryIcfImpl;
	@Autowired(required = true)
	private PrismContext prismContext;
	
	private static Trace LOGGER = TraceManager.getTrace(TestUcfDummy.class);
	
	@BeforeClass
	public void setup() throws SchemaException, SAXException, IOException {
		displayTestTile("setup");
		System.setProperty("midpoint.home", "target/midPointHome/");

		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		
		manager = connectorFactoryIcfImpl;

		resource = PrismTestUtil.parseObject(new File(FILENAME_RESOURCE_DUMMY));
		resourceType = resource.asObjectable();

		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(new File (FILENAME_CONNECTOR_DUMMY));
		connectorType = connector.asObjectable();
	}
		
	@Test
	public void test000PrismContextSanity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000PrismContextSanity");
		
		SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();
		PrismSchema schemaIcfc = schemaRegistry.findSchemaByNamespace(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION);
		assertNotNull("ICFC schema not found in the context ("+ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION+")", schemaIcfc);
		PrismContainerDefinition configurationPropertiesDef = 
			schemaIcfc.findContainerDefinitionByElementName(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("icfc:configurationProperties not found in icfc schema ("+
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME+")", configurationPropertiesDef);
		PrismSchema schemaIcfs = schemaRegistry.findSchemaByNamespace(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA);
		assertNotNull("ICFS schema not found in the context ("+ConnectorFactoryIcfImpl.NS_ICF_SCHEMA+")", schemaIcfs);
	
		//"http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/configuration-1.xsd";
	}
	
	@Test
	public void test001ResourceSanity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test001ResourceSanity");
		
		display("Resource", resource);
		
		assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", "Dummy Resource");
		assertPropertyDefinition(resource, "name", DOMUtil.XSD_STRING, 0, 1);		
				
		PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", 1, configItems.size());
		
		PrismContainer<?> dummyConfigPropertiesContainer = configurationContainer.findContainer(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No icfc:configurationProperties container", dummyConfigPropertiesContainer);
		List<Item<?>> dummyConfigPropItems = dummyConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of dummy ConfigPropItems items", 3, dummyConfigPropItems.size());
	}

	@Test
	public void test002ConnectorSchema() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test002ConnectorSchema");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType));
		assertNotNull("Failed to instantiate connector", cc);
		PrismSchema connectorSchema = cc.generateConnectorSchema();
		ProvisioningTestUtil.assertConnectorSchemaSanity(connectorSchema, "generated");
		assertEquals("Unexpected number of definitions", 3, connectorSchema.getDefinitions().size());

		
		Document xsdSchemaDom = connectorSchema.serializeToXsd();
		assertNotNull("No serialized connector schema", xsdSchemaDom);
		display("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));
		
		// Try to re-parse
		PrismSchema reparsedConnectorSchema = PrismSchema.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), "schema fetched from "+cc, PrismTestUtil.getPrismContext());
		ProvisioningTestUtil.assertConnectorSchemaSanity(reparsedConnectorSchema, "re-parsed");
		assertEquals("Unexpected number of definitions in re-parsed schema", 3, reparsedConnectorSchema.getDefinitions().size());		
	}
	
	/**
	 * Test listing connectors. Very simple. Just test that the list is
	 * non-empty and that there are mandatory values filled in.
	 * @throws CommunicationException 
	 */
	@Test
	public void test010ListConnectors() throws CommunicationException {
		displayTestTile("test004ListConnectors");
		
		OperationResult result = new OperationResult(TestUcfDummy.class+".testListConnectors");
		Set<ConnectorType> listConnectors = manager.listConnectors(null, result);

		System.out.println("---------------------------------------------------------------------");
		assertNotNull(listConnectors);
		assertFalse(listConnectors.isEmpty());

		for (ConnectorType connector : listConnectors) {
			assertNotNull(connector.getName());
			System.out.println("CONNECTOR OID=" + connector.getOid() + ", name=" + connector.getName() + ", version="
					+ connector.getConnectorVersion());
			System.out.println("--");
			System.out.println(ObjectTypeUtil.dump(connector));
			System.out.println("--");
		}

		System.out.println("---------------------------------------------------------------------");

	}
	
	@Test
	public void test020CreateConfiguredConnector() throws FileNotFoundException, JAXBException,
			ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ConfigurationException {
		displayTestTile("test004CreateConfiguredConnector");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType));
		assertNotNull("Failed to instantiate connector", cc);
		OperationResult result = new OperationResult(TestUcfDummy.class.getName() + ".testCreateConfiguredConnector");
		PrismContainerValue configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
		display("Configuration container", configContainer);
		
		// WHEN
		cc.configure(configContainer, result);
		
		// THEN
		result.computeStatus("test failed");
		assertSuccess("Connector configuration failed", result);
		// TODO: assert something
	}
	
	@Test
	public void test030ResourceSchema() throws ObjectNotFoundException, SchemaException, CommunicationException, GenericFrameworkException, ConfigurationException {
		displayTestTile("test030ResourceSchema");
		
		OperationResult result = new OperationResult(TestUcfDummy.class+".test030ResourceSchema");
		
		cc = manager.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType));
		assertNotNull("Failed to instantiate connector", cc);
		
		PrismContainerValue configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
		display("Configuration container", configContainer);
		cc.configure(configContainer, result);
		
		// WHEN
		resourceSchema = cc.getResourceSchema(result);
		
		// THEN
		display("Generated resource schema", resourceSchema);
		assertEquals("Unexpected number of definitions", 1, resourceSchema.getDefinitions().size());
		
		ProvisioningTestUtil.assertDummyResourceSchemaSanity(resourceSchema, resourceType);
		
		Document xsdSchemaDom = resourceSchema.serializeToXsd();
		assertNotNull("No serialized resource schema", xsdSchemaDom);
		display("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));
		
		// Try to re-parse
		ResourceSchema reparsedResourceSchema = ResourceSchema.parse(DOMUtil.getFirstChildElement(xsdSchemaDom),
				"serialized schema", PrismTestUtil.getPrismContext());
		assertEquals("Unexpected number of definitions in re-parsed schema", 1, reparsedResourceSchema.getDefinitions().size());
		
		ProvisioningTestUtil.assertDummyResourceSchemaSanity(reparsedResourceSchema, resourceType);
	}
	
	@Test
	public void test040AddAccount() throws Exception {
		displayTestTile(this, "test040AddAccount");

		OperationResult result = new OperationResult(this.getClass().getName() + ".test040AddAccount");

		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultAccountDefinition();
		AccountShadowType shadowType = new AccountShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(ACCOUNT_JACK_USERNAME);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<AccountShadowType> shadow = shadowType.asPrismObject();
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getOrCreateAttributesContainer(shadow, defaultAccountDefinition);
		ResourceAttribute<String> icfsNameProp = attributesContainer.findOrCreateAttribute(ConnectorFactoryIcfImpl.ICFS_NAME);
		icfsNameProp.setRealValue(ACCOUNT_JACK_USERNAME);
		
		// WHEN
		cc.addObject(shadow, null, result);

		// THEN
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_USERNAME);
		assertNotNull("Account "+ACCOUNT_JACK_USERNAME+" was not created", dummyAccount);
		assertNotNull("Account "+ACCOUNT_JACK_USERNAME+" has no username", dummyAccount.getUsername());
		
	}
	
	@Test
	public void test050Search() throws UcfException, SchemaException, CommunicationException {
		displayTestTile("test050Search");
		// GIVEN

		final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultAccountDefinition();
		// Determine object class from the schema
		
		final List<PrismObject<AccountShadowType>> searchResults = new ArrayList<PrismObject<AccountShadowType>>();

		ResultHandler<AccountShadowType> handler = new ResultHandler<AccountShadowType>() {

			@Override
			public boolean handle(PrismObject<AccountShadowType> shadow) {
				System.out.println("Search: found: " + shadow);
				checkUcfShadow(shadow, accountDefinition);
				searchResults.add(shadow);
				return true;
			}
		};

		OperationResult result = new OperationResult(this.getClass().getName() + ".testSearch");

		// WHEN
		cc.search(AccountShadowType.class, accountDefinition, new ObjectQuery(), handler, result);

		// THEN
		assertEquals("Unexpected number of search results", 1, searchResults.size());
	}
	
	private void checkUcfShadow(PrismObject<AccountShadowType> shadow, ObjectClassComplexTypeDefinition objectClassDefinition) {
		assertNotNull("No objectClass in shadow "+shadow, shadow.asObjectable().getObjectClass());
		assertEquals("Wrong objectClass in shadow "+shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
		Collection<ResourceAttribute<?>> attributes = ResourceObjectShadowUtil.getAttributes(shadow);
		assertNotNull("No attributes in shadow "+shadow, attributes);
		assertFalse("Empty attributes in shadow "+shadow, attributes.isEmpty());
	}
	
	@Test
	public void test100FetchEmptyChanges() throws Exception {
		displayTestTile(this, "test100FetchEmptyChanges");

		OperationResult result = new OperationResult(this.getClass().getName() + ".test100FetchEmptyChanges");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultAccountDefinition();
		
		// WHEN
		PrismProperty<?> lastToken = cc.fetchCurrentToken(accountDefinition, result);

		assertNotNull("No last sync token", lastToken);
		
		System.out.println("Property:");
		System.out.println(lastToken.dump());
		
		PrismPropertyDefinition lastTokenDef = lastToken.getDefinition();
		assertNotNull("No last sync token definition", lastTokenDef);
		assertEquals("Last sync token definition has wrong type", DOMUtil.XSD_INT, lastTokenDef.getTypeName());
		assertTrue("Last sync token definition is NOT dynamic", lastTokenDef.isDynamic());
		
		// WHEN
		List<Change> changes = cc.fetchChanges(accountDefinition, lastToken, result);
		
		AssertJUnit.assertEquals(0, changes.size());
	}
	
	@Test
	public void test101FetchAddChange() throws Exception {
		displayTestTile(this, "test101FetchAddChange");

		OperationResult result = new OperationResult(this.getClass().getName() + ".test101FetchAddChange");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultAccountDefinition();
		
		PrismProperty<?> lastToken = cc.fetchCurrentToken(accountDefinition, result);
		assertNotNull("No last sync token", lastToken);

		// Add account to the resource
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount("blackbeard");
		newAccount.addAttributeValues("fullname", "Edward Teach");
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);
		
		// WHEN
		List<Change> changes = cc.fetchChanges(accountDefinition, lastToken, result);
		
		AssertJUnit.assertEquals(1, changes.size());
		Change change = changes.get(0);
		assertNotNull("null change", change);
		PrismObject<? extends ResourceObjectShadowType> currentShadow = change.getCurrentShadow();
		assertNotNull("null current shadow", currentShadow);
		PrismAsserts.assertParentConsistency(currentShadow);
		Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
		assertNotNull("null identifiers", identifiers);
		assertFalse("empty identifiers", identifiers.isEmpty());
		
	}
	
	


	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}
	
	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}
}
