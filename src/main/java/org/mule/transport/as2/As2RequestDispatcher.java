package org.mule.transport.as2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.config.MutableThreadingProfile;
import org.mule.transport.ConnectException;
import org.mule.transport.http.HttpConnector;
import org.mule.util.concurrent.ThreadNameHelper;

public class As2RequestDispatcher implements Work{
	
    private static Log logger = LogFactory.getLog(As2RequestDispatcher.class);

    private ServerSocket serverSocket;
    private As2Connector as2Connector;
    private RetryPolicyTemplate retryTemplate;
    protected ExecutorService requestHandOffExecutor;
    private WorkManager workManager;
    private final AtomicBoolean disconnect = new AtomicBoolean(false);

    public As2RequestDispatcher(final As2Connector as2Connector, final RetryPolicyTemplate retryPolicyTemplate, final ServerSocket serverSocket, final WorkManager workManager)
    {
        if (as2Connector == null)
        {
            throw new IllegalArgumentException("As2Connector can not be null");
        }
        if (retryPolicyTemplate == null)
        {
            throw new IllegalArgumentException("RetryPolicyTemplate can not be null");
        }
        if (serverSocket == null)
        {
            throw new IllegalArgumentException("ServerSocket can not be null");
        }
        if (workManager == null)
        {
            throw new IllegalArgumentException("WorkManager can not be null");
        }
        this.as2Connector = as2Connector;
        this.retryTemplate = retryPolicyTemplate;
        this.serverSocket = serverSocket;
        this.workManager = workManager;
        this.requestHandOffExecutor = createRequestDispatcherThreadPool(as2Connector);
    }

	private ExecutorService createRequestDispatcherThreadPool(As2Connector as2Connector)
    {
        ThreadingProfile receiverThreadingProfile = as2Connector.getReceiverThreadingProfile();
        MutableThreadingProfile dispatcherThreadingProfile = new MutableThreadingProfile(receiverThreadingProfile);
        dispatcherThreadingProfile.setThreadFactory(null);
        dispatcherThreadingProfile.setMaxThreadsActive(dispatcherThreadingProfile.getMaxThreadsActive() * 2);
        String threadNamePrefix = ThreadNameHelper.getPrefix(as2Connector.getMuleContext()) + "http.request.dispatch." + serverSocket.getLocalPort();
        ExecutorService executorService = dispatcherThreadingProfile.createPool(threadNamePrefix);
        return executorService;
    }

    @Override
    public void run()
    {
        while (!disconnect.get())
        {
            if (as2Connector.isStarted() && !disconnect.get())
            {
                try
                {
                    retryTemplate.execute(new RetryCallback()
                    {
                        public void doWork(RetryContext context) throws Exception
                        {
                            Socket socket = null;
                            try
                            {
                                socket = serverSocket.accept();
                            }
                            catch (Exception e)
                            {
                                if (!as2Connector.isDisposed() && !disconnect.get())
                                {
                                    throw new ConnectException(e, null);
                                }
                            }

                            if (socket != null)
                            {
                                final Runnable as2RequestDispatcherWork = new As2RequestDispatcherWork(as2Connector, socket);
                                // Process each connection in a different thread so we can continue accepting connection right away.
                                requestHandOffExecutor.execute(as2RequestDispatcherWork);
                            }
                        }

                        public String getWorkDescription()
                        {
                            String hostName = ((InetSocketAddress) serverSocket.getLocalSocketAddress()).getHostName();
                            int port = ((InetSocketAddress) serverSocket.getLocalSocketAddress()).getPort();
                            return String.format("%s://%s:%d", as2Connector.getProtocol(), hostName, port);
                        }
                    }, workManager);
                }
                catch (Exception e)
                {
                    as2Connector.getMuleContext().getExceptionListener().handleException(e);
                }
            }
        }
    }

    @Override
    public void release()
    {

    }

    void disconnect()
    {
        disconnect.set(true);
        try
        {
            if (serverSocket != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing: " + serverSocket);
                }
                serverSocket.close();
            }
        }
        catch (IOException e)
        {
            logger.warn("Failed to close server socket: " + e.getMessage(), e);
        }
    }
}
