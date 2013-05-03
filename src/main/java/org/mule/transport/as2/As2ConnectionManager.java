package org.mule.transport.as2;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.EndpointURI;
import org.mule.transport.ConnectException;


class As2ConnectionManager {
	
	private static final int LAST_CONNECTION = 1;
    protected final Log logger = LogFactory.getLog(getClass());
    final private As2Connector connector;
    final private Map<String, As2RequestDispatcher> socketDispatchers = new HashMap<String, As2RequestDispatcher>();
    final private Map<String, Integer> socketDispatcherCount = new HashMap<String, Integer>();
    final private WorkManager workManager;

    public As2ConnectionManager(As2Connector connector, WorkManager workManager)
    {
        if (connector == null)
        {
            throw new IllegalArgumentException("HttpConnector can not be null");
        }
        if (workManager == null)
        {
            throw new IllegalArgumentException("WorkManager can not be null");
        }
        this.connector = connector;
        this.workManager = workManager;
    }

    synchronized void addConnection(final EndpointURI endpointURI) throws ConnectException
    {
        try
        {
            String endpointKey = getKeyForEndpointUri(endpointURI);
            if (socketDispatchers.containsKey(endpointKey))
            {
                socketDispatcherCount.put(endpointKey, socketDispatcherCount.get(endpointKey) + 1);
            }
            else
            {
                ServerSocket serverSocket = connector.getServerSocket(endpointURI.getUri());
                As2RequestDispatcher as2RequestDispatcher = new As2RequestDispatcher(connector, connector.getRetryPolicyTemplate(), serverSocket, workManager);
                socketDispatchers.put(endpointKey, as2RequestDispatcher);
                socketDispatcherCount.put(endpointKey, new Integer(1));
                workManager.scheduleWork(as2RequestDispatcher, WorkManager.INDEFINITE, null, connector);
            }
        }
        catch (Exception e)
        {
            throw new ConnectException(e, connector);
        }
    }

    synchronized void removeConnection(final EndpointURI endpointURI)
    {
        String endpointKey = getKeyForEndpointUri(endpointURI);
        if (!socketDispatchers.containsKey(endpointKey))
        {
            logger.warn("Trying to disconnect endpoint with uri " + endpointKey + " but " + As2RequestDispatcher.class.getName() + " does not exists for that uri");
            return;
        }
        Integer connectionsRequested = socketDispatcherCount.get(endpointKey);
        if (connectionsRequested == LAST_CONNECTION)
        {
            As2RequestDispatcher as2RequestDispatcher = socketDispatchers.get(endpointKey);
            as2RequestDispatcher.disconnect();
            socketDispatchers.remove(endpointKey);
            socketDispatcherCount.remove(endpointKey);
        }
        else
        {
            socketDispatcherCount.put(endpointKey, socketDispatcherCount.get(endpointKey) - 1);
        }
    }

    private String getKeyForEndpointUri(final EndpointURI endpointURI)
    {
        return endpointURI.getHost() + ":" + endpointURI.getPort();
    }

    public void dispose()
    {
        for (As2RequestDispatcher as2RequestDispatcher : socketDispatchers.values())
        {
            as2RequestDispatcher.disconnect();
        }
        socketDispatchers.clear();
        socketDispatcherCount.clear();
        workManager.dispose();
    }
}
