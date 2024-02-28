package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.QADocPublish

class TEST_QADocPublish {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {
        def agent = new QADocPublish()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST06BPM_QA243fd78b86-ed4c-465a-b36d-6e9f6a009074182024-02-27T09:59:36.921Z013"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        assert result.resultCode == 0
    }

    @Test
    void testForJavaAgentMethod() {
        //def agent = new JavaAgent()
        //agent.initializeGroovyBlueline(binding.variables)
        //assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
