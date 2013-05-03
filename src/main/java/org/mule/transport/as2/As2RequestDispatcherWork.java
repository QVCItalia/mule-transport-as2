package org.mule.transport.as2;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpMessageReceiver;
import org.mule.transport.http.HttpRequestDispatcherWork;
import org.mule.transport.http.HttpServerConnection;
import org.mule.transport.http.RequestLine;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.monitor.Expirable;

public class As2RequestDispatcherWork implements Runnable, Expirable {

    private static Log logger = LogFactory.getLog(HttpRequestDispatcherWork.class);

    private HttpServerConnection httpServerConnection;
    private Socket socket;
    private As2Connector as2Connector;

    public As2RequestDispatcherWork(As2Connector as2Connector, Socket socket)
    {
        if (as2Connector == null)
        {
            throw new IllegalArgumentException("As2Connector can not be null");
        }
        if (socket == null)
        {
            throw new IllegalArgumentException("Socket can not be null");
        }
        this.as2Connector = as2Connector;
        this.socket = socket;
    }

    @Override
    public void run()
    {
        try
        {
            long keepAliveTimeout = as2Connector.getKeepAliveTimeout();
            String encoding = as2Connector.getMuleContext().getConfiguration().getDefaultEncoding();
            httpServerConnection = new HttpServerConnection(socket, encoding, as2Connector);
            do
            {
                try
                {

                    httpServerConnection.setKeepAlive(false);

                    // Only add a monitor if the timeout has been set
                    if (keepAliveTimeout > 0)
                    {
                        as2Connector.getKeepAliveMonitor().addExpirable(
                                keepAliveTimeout, TimeUnit.MILLISECONDS, this);
                    }

                    RequestLine requestLine = httpServerConnection.getRequestLine();
                    if (requestLine != null)
                    {
                        try
                        {
                            As2MessageReceiver as2MessageReceiver = (As2MessageReceiver) as2Connector.lookupReceiver(socket, requestLine);
                            as2MessageReceiver.processRequest(httpServerConnection);
                        }
                        catch (NoReceiverForEndpointException e)
                        {
                            httpServerConnection.writeFailureResponse(HttpConstants.SC_NOT_FOUND, HttpMessages.cannotBindToAddress(httpServerConnection.getFullUri()).toString());
                        }
                    }
                }
                finally
                {
                    as2Connector.getKeepAliveMonitor().removeExpirable(this);
                    httpServerConnection.reset();
                }
            }
            while (httpServerConnection.isKeepAlive());
        }
        catch (As2MessageReceiver.EmptyRequestException e)
        {
            logger.debug("Discarding request since content was empty");
        }
        catch (As2MessageReceiver.FailureProcessingRequestException e)
        {
            logger.debug("Closing socket due to failure during request processing");
        }
        catch (Exception e)
        {
            as2Connector.getMuleContext().getExceptionListener().handleException(e);
        }
        finally
        {
            logger.debug("Closing HTTP connection.");
            if (httpServerConnection != null && httpServerConnection.isOpen())
            {
                httpServerConnection.close();
                httpServerConnection = null;
            }
        }
    }

    @Override
    public void expired()
    {
        if (httpServerConnection.isOpen())
        {
            httpServerConnection.close();
        }
    }

	
}
