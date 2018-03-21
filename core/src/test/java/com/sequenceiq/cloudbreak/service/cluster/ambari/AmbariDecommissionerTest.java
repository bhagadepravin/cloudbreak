package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;

@RunWith(MockitoJUnitRunner.class)
public class AmbariDecommissionerTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private AmbariDecommissioner underTest;

    @Mock
    private Stack stack;

    @Mock
    private InstanceGroup instanceGroup;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test to prove that if the Ambari IP and the node's (which is going to be downscaled) public ip is diffent, then there shouldn't invoke any exception,
     * which means that the selected node is not the one which serves as a host for the Ambari server.
     */
    @Test
    public void testVerifyNodeIsNotAmbariServerWherePublicIpAndAmbariIpDifferent() {
        String ambariIp = "135.195.251.131";
        String nodePublicIp = "10.10.10.10";
        String nodeName = "node name";

        when(stack.getAmbariIp()).thenReturn(ambariIp);
        when(stack.getInstanceGroups()).thenReturn(createInstanceGroups(instanceGroup));
        when(instanceGroup.getInstanceMetaData()).thenReturn(createInstanceMetaDataSet(instanceMetaData));
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(nodeName);
        when(instanceMetaData.getPublicIp()).thenReturn(nodePublicIp);

        underTest.verifyNodeIsNotAmbariServer(stack, nodeName);
    }

    /**
     * Test which proves that if the Ambari IP and the downscalable node's IP is the same, than an exception would invoke, reporting that the
     * downscale is not available for that node because that node is the host for the Ambari server which is obviously can't be downscaled.
     */
    @Test
    public void testVerifyNodeIsNotAmbariServerWherePublicIpAndAmbariIpAreTheSame() {
        String commonIpAddress = "135.195.251.131";
        String nodeName = "node name";

        when(stack.getAmbariIp()).thenReturn(commonIpAddress);
        when(stack.getInstanceGroups()).thenReturn(createInstanceGroups(instanceGroup));
        when(instanceGroup.getInstanceMetaData()).thenReturn(createInstanceMetaDataSet(instanceMetaData));
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(nodeName);
        when(instanceMetaData.getPublicIp()).thenReturn(commonIpAddress);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Downscale is prohibited: the node which has selected to downscale maintains the Ambari server");

        underTest.verifyNodeIsNotAmbariServer(stack, nodeName);
    }

    /**
     * Test for that if Stack instance does not contains any node with a FQDN which matches the provided node FQDN value.
     * If it so, an exception would invoke indicating that there are data inconsistency in the incoming data.
     */
    @Test
    public void testNodePublicIpExtractionAttemptWhenThereIsNotAnyNodeWithTheProvidedOne() {
        when(stack.getInstanceGroups()).thenReturn(createInstanceGroups(instanceGroup));
        when(instanceGroup.getInstanceMetaData()).thenReturn(createInstanceMetaDataSet(instanceMetaData));
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn("node name");

        String differentNodeName = "And Now for Something Completely Different";
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(String.format("Could not find the expected \"%s\" host by it's name", differentNodeName));

        underTest.verifyNodeIsNotAmbariServer(stack, differentNodeName);
    }

    private Set<InstanceGroup> createInstanceGroups(InstanceGroup... instances) {
        Set<InstanceGroup> instanceGroups = new HashSet<>(instances.length);
        instanceGroups.addAll(Arrays.asList(instances));
        return instanceGroups;
    }

    private Set<InstanceMetaData> createInstanceMetaDataSet(InstanceMetaData... metaData) {
        Set<InstanceMetaData> metaDataSet = new HashSet<>(metaData.length);
        metaDataSet.addAll(Arrays.asList(metaData));
        return metaDataSet;
    }

}
