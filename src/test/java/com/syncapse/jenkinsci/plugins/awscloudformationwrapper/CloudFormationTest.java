package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;

@RunWith(MockitoJUnitRunner.class)
public class CloudFormationTest {
	
	private CloudFormation cf;	// SUT
	private String cloudFormationRecipe;
	private Map<String, String> parameters;
	private String awsAccessKey = "accessKey";
	private String awsSecretKey = "secretKey";
	@Mock protected AmazonCloudFormation awsClient;

	@Before
	public void setup() throws Exception{
		
		cf = new CloudFormation(System.out, "testStack", cloudFormationRecipe, parameters, 200, awsAccessKey, awsSecretKey){
			@Override
			protected AmazonCloudFormation getAWSClient(){
				return awsClient;
			}
		};

		when(awsClient.createStack(any(CreateStackRequest.class))).thenReturn(createResultWithId("testStack"));
		when(awsClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(new DescribeStackEventsResult());
		
	}

	@Test
	public void cloudFormationCreate_Wait_for_Stack_To_Be_Created() throws Exception{

		when(awsClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(stackPendingResult(), stackPendingResult(), stackCompletedResult());
		cf.create();
		verify(awsClient, times(3)).describeStacks(any(DescribeStacksRequest.class));

	}
	
	@Test
	public void create_returns_null_when_stack_creation_fails() throws Exception {
		when(awsClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(stackFailedResult());
		assertNull(cf.create());
	}
	
	@Test
	public void delete_waits_for_stack_to_be_deleted() throws Exception {
		when(awsClient.describeStacks()).thenReturn(stackDeletingResult(), stackDeletingResult(), stackDeleteSuccessfulResult());
		cf.delete();
		verify(awsClient, times(3)).describeStacks();
	}

	private DescribeStacksResult stackDeleteSuccessfulResult() {
		return new DescribeStacksResult(); // A result with no stacks in it.
	}

	private DescribeStacksResult stackDeletingResult() {
		return describeStacksResultWithStatus(StackStatus.DELETE_IN_PROGRESS);
	}

	private DescribeStacksResult stackFailedResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_FAILED);
	}

	private CreateStackResult createResultWithId(String stackId) {
		return new CreateStackResult().withStackId(stackId);
	}

	private DescribeStacksResult stackCompletedResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_COMPLETE);
	}

	private DescribeStacksResult stackPendingResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_IN_PROGRESS);
	}

	private DescribeStacksResult describeStacksResultWithStatus(StackStatus status) {
		return new DescribeStacksResult().withStacks(new Stack().withStackStatus(status.name()).withStackName("testStack"));
	}

}