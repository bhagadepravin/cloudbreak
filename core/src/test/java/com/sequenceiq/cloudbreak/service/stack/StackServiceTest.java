package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@RunWith(MockitoJUnitRunner.class)
public class StackServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "instanceId";

    private static final String AMBARI_IP = "1.1.1.1";

    private static final String INSTANCE_IP = "2.2.2.2";

    private static final String IDENTITY_USER_ID = "user id";

    private static final String STACK_OWNER = "some other user which does not equals to the Identity user's ID";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private Stack stack;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private IdentityUser user;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test for that when every checking operation during the remove instance call works properly and there is no problem with the Stack and the
     * InstanceMetaData nor the selected instance is not the host of the Ambari server.
     */
    @Test
    public void testRemoveInstanceWhenTheInstanceIsNotTheHostForTheAmbariServier() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_IP);
        when(stack.getAmbariIp()).thenReturn(AMBARI_IP);
        when(user.getUserId()).thenReturn(IDENTITY_USER_ID);
        when(stack.getOwner()).thenReturn(STACK_OWNER);
        when(stack.isPublicInAccount()).thenReturn(true);
        doNothing().when(flowManager).triggerStackRemoveInstance(anyLong(), anyString(), anyString());

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
    }

    /**
     * Test which proves that the user can't delete that instance which serves as the Ambari server host. If the instance which selected for delete have the
     * same public ip address as the Ambari server, than the delete operation should terminated and an exception would be invoked indicating that the user
     * tried to delete the instance where the Ambari server is.
     */
    @Test
    public void testRemoveInstanceWhenTheInstanceIsTheOneWhichTheHostForTheAmbariServer() {
        String instanceDiscoveryFQDN = "node FQDN";
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getPublicIp()).thenReturn(AMBARI_IP);
        when(stack.getAmbariIp()).thenReturn(AMBARI_IP);
        when(user.getUserId()).thenReturn(IDENTITY_USER_ID);
        when(stack.getOwner()).thenReturn(STACK_OWNER);
        when(stack.isPublicInAccount()).thenReturn(true);
        doNothing().when(flowManager).triggerStackRemoveInstance(anyLong(), anyString(), anyString());
        when(instanceMetaData.getDiscoveryFQDN()).thenReturn(instanceDiscoveryFQDN);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(String.format("Downscale for node [%s] is prohibited because it maintains the Ambari server", instanceDiscoveryFQDN));

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
    }

    /**
     * Test for that when a private (Stack) user which does not owns the instance would like to terminate it which scenario is not acceptable hence the process
     * should be stopped.
     */
    @Test
    public void testWhenStackIsNotPublicAndItsOwnerIsNotTheIdentityUser() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_IP);
        when(stack.getAmbariIp()).thenReturn(AMBARI_IP);
        when(user.getUserId()).thenReturn(IDENTITY_USER_ID);
        when(stack.getOwner()).thenReturn(STACK_OWNER);
        when(stack.isPublicInAccount()).thenReturn(false);
        doNothing().when(flowManager).triggerStackRemoveInstance(anyLong(), anyString(), anyString());

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(String.format("Private stack (%s) only modifiable by the owner.", STACK_ID));

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
    }

    /**
     * Test where the private Stack user and the Identity user is the same tries to delete a/the instance which is acceptable and the deletion.
     */
    @Test
    public void testWhenStackIsNotPublicAndItsOwnerIsTheIdentityUserItself() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(instanceMetaData);
        when(instanceMetaData.getPublicIp()).thenReturn(INSTANCE_IP);
        when(stack.getAmbariIp()).thenReturn(AMBARI_IP);
        when(user.getUserId()).thenReturn(IDENTITY_USER_ID);
        when(stack.getOwner()).thenReturn(IDENTITY_USER_ID);
        when(stack.isPublicInAccount()).thenReturn(false);
        doNothing().when(flowManager).triggerStackRemoveInstance(anyLong(), anyString(), anyString());

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
        verify(instanceMetaDataRepository, times(1)).findByInstanceId(STACK_ID, INSTANCE_ID);
    }

    /**
     * Tests that if the returning InstanceMetaData is null (not found or for some other reasons) which prevents the delete operation therefore an exception
     * would be thrown indicating that the metadata could not be extracted from the given data.
     */
    @Test
    public void testWhenInstanceMetaDataIsNull() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(stack);
        when(instanceMetaDataRepository.findByInstanceId(STACK_ID, INSTANCE_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Metadata for instance %s not found.", INSTANCE_ID));

        underTest.removeInstance(user, STACK_ID, INSTANCE_ID);
    }

    /**
     * Test that if the retrieved Stack instance from the StackRepository is null, then the other activities would be stopped because the there is no such
     * Stack like the one with the provided stack ID and that could cause malfunctions.
     */
    @Test
    public void testWhenStackCouldNotFindByItsId() {
        when(stackRepository.findOne(STACK_ID)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(String.format("Stack '%s' not found", STACK_ID));

        underTest.get(STACK_ID);
    }

}
