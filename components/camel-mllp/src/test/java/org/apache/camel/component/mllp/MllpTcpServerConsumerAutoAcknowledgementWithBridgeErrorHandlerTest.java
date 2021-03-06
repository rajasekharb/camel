/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.mllp;

import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;

import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MllpTcpServerConsumerAutoAcknowledgementWithBridgeErrorHandlerTest
        extends TcpServerConsumerAcknowledgementTestSupport {
    @Override
    protected boolean isBridgeErrorHandler() {
        return true;
    }

    @Override
    protected boolean isAutoAck() {
        return true;
    }

    @Test
    public void testReceiveSingleMessage() throws Exception {
        result.expectedBodiesReceived(TEST_MESSAGE);

        complete.expectedBodiesReceived(TEST_MESSAGE);
        complete.expectedHeaderReceived(MllpConstants.MLLP_ACKNOWLEDGEMENT_TYPE, "AA");

        receiveSingleMessage();

        Exchange completeExchange = complete.getReceivedExchanges().get(0);

        assertNotNull(completeExchange.getIn().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT));
        assertNotNull(completeExchange.getIn().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING));

        String acknowledgement = completeExchange.getIn().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING, String.class);

        assertThat(acknowledgement, startsWith("MSH|^~\\&|^org^sys||APP_A|FAC_A|"));
        assertThat(acknowledgement, endsWith("||ACK^A04^ADT_A04|||2.6\rMSA|AA|\r"));
    }

    public void testAcknowledgementDeliveryFailure() throws Exception {
        result.expectedBodiesReceived(TEST_MESSAGE);

        failure.expectedBodiesReceived(TEST_MESSAGE);
        failure.expectedHeaderReceived(MllpConstants.MLLP_ACKNOWLEDGEMENT_TYPE, "AA");
        failure.expectedHeaderReceived(MllpConstants.MLLP_ACKNOWLEDGEMENT, EXPECTED_ACKNOWLEDGEMENT);
        failure.expectedHeaderReceived(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING, EXPECTED_ACKNOWLEDGEMENT);

        acknowledgementDeliveryFailure();

        Exchange failureExchange = failure.getExchanges().get(0);
        Object failureException = failureExchange.getProperty(MllpConstants.MLLP_ACKNOWLEDGEMENT_EXCEPTION);
        assertNotNull(failureException,
                "OnFailureOnly exchange should have a " + MllpConstants.MLLP_ACKNOWLEDGEMENT_EXCEPTION + " property");
        assertIsInstanceOf(Exception.class, failureException);
    }

    @Test
    public void testUnparsableMessage() throws Exception {
        final String testMessage = "MSH" + TEST_MESSAGE;

        result.expectedBodiesReceived(testMessage);
        complete.expectedMessageCount(1);
        ackGenerationEx.expectedMessageCount(1);

        unparsableMessage(testMessage);

        assertNull(result.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");
        assertNull(complete.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");

        assertNotNull(ackGenerationEx.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should have the exception in the exchange property");
    }

    @Test
    public void testMessageWithEmptySegment() throws Exception {
        final String testMessage = TEST_MESSAGE.replace("\rPID|", "\r\rPID|");

        result.expectedBodiesReceived(testMessage);
        complete.expectedMessageCount(1);

        unparsableMessage(testMessage);

        assertNull(result.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");
        assertNull(complete.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");
    }

    @Test
    public void testMessageWithEmbeddedNewlines() throws Exception {
        final String testMessage = TEST_MESSAGE.replace("\rPID|", "\r\n\rPID|\n");

        result.expectedBodiesReceived(testMessage);
        complete.expectedMessageCount(1);

        unparsableMessage(testMessage);

        assertNull(result.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");
        assertNull(complete.getReceivedExchanges().get(0).getProperty(Exchange.EXCEPTION_CAUGHT),
                "Should not have the exception in the exchange property");
    }
}
