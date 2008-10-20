package org.jboss.tools.project.examples.filetransfer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.ecf.filetransfer.IRemoteFile;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemBrowserContainerAdapter;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.RemoteFileSystemException;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.ecf.filetransfer.events.IFileTransferEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDataEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDoneEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemBrowseEvent;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemEvent;
import org.eclipse.ecf.filetransfer.identity.FileCreateException;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.ecf.provider.filetransfer.retrieve.AbstractRetrieveFileTransfer;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI.AuthenticationInfo;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.jboss.tools.project.examples.ProjectExamplesActivator;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author snjeza
 * based on org.eclipse.equinox.internal.p2.updatesite.ECFTransport
 * 
 */
public class ECFExamplesTransport {
	
	/**
	 * The number of password retry attempts allowed before failing.
	 */
	private static final int LOGIN_RETRIES = 3;
	private static final ProtocolException ERROR_401 = new ProtocolException();
	private static final String SERVER_REDIRECT = "Server redirected too many times";
	
	private static ECFExamplesTransport INSTANCE;
	private ServiceTracker retrievalFactoryTracker;
	
	/**
	 * A job that waits on a barrier.
	 */
	static class WaitJob extends Job {
		private final Object[] barrier;

		/**
		 * Creates a wait job.
		 * @param location A location string that is used in the job name
		 * @param barrier The job will wait until the first entry in the barrier is non-null
		 */
		WaitJob(String location, Object[] barrier) {
			super("Loading");
			this.barrier = barrier;
			setSystem(true);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (barrier) {
				while (barrier[0] == null) {
					try {
						barrier.wait();
					} catch (InterruptedException e) {
						//ignore
					}
				}
			}
			return Status.OK_STATUS;
		}
	}

	/**
	 * Private to avoid client instantiation.
	 */
	private ECFExamplesTransport() {
		retrievalFactoryTracker = new ServiceTracker(ProjectExamplesActivator.getBundleContext(), IRetrieveFileTransferFactory.class.getName(), null);
		retrievalFactoryTracker.open();
	}
	
	/**
	 * Returns an initialized instance of ECFExamplesTransport
	 */
	public static synchronized ECFExamplesTransport getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new ECFExamplesTransport();
		}
		return INSTANCE;
	}
	
	/**
	 * Gets the last modified date for the specified file.
	 * @param location - The URL location of the file.
	 * @return A <code>long</code> representing the date. Returns <code>0</code> if the file is not found or an error occurred.
	 * @exception OperationCanceledException if the request was canceled.
	 */
	public long getLastModified(URL location) throws CoreException {
		String locationString = location.toExternalForm();
		try {
			IConnectContext context = getConnectionContext(locationString, false);
			for (int i = 0; i < LOGIN_RETRIES; i++) {
				try {
					return doGetLastModified(locationString, context);
				} catch (ProtocolException e) {
					if (ERROR_401 == e)
						context = getConnectionContext(locationString, true);
				} catch (Exception e) {
					e.getMessage();
				}
			}
		} catch (UserCancelledException e) {
			throw new OperationCanceledException();
		}
		//too many retries, so report as failure
		throw new CoreException(new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID, "IO error", null));
	}
	
	/**
	 * Perform the ECF call to get the last modified time, failing if there is any
	 * protocol failure such as an authentication failure.
	 */
	private long doGetLastModified(String location, IConnectContext context) throws ProtocolException {
		IContainer container;
		try {
			container = ContainerFactory.getDefault().createContainer();
		} catch (ContainerCreateException e) {
			return 0;
		}
		IRemoteFileSystemBrowserContainerAdapter adapter = (IRemoteFileSystemBrowserContainerAdapter) container.getAdapter(IRemoteFileSystemBrowserContainerAdapter.class);
		if (adapter == null) {
			return 0;
		}
		IRemoteFile remoteFile = checkFile(adapter, location, context);
		if (remoteFile == null) {
			return 0;
		}
		return remoteFile.getInfo().getLastModified();
	}
	
	/**
	 * Downloads the contents of the given URL to the given output stream. The
	 * destination stream will be closed by this method whether it succeeds
	 * to download or not.
	 */
	public IStatus download(String name,String url, OutputStream destination, IProgressMonitor monitor) {
		try {
			IConnectContext context = getConnectionContext(url, false);
			for (int i = 0; i < LOGIN_RETRIES; i++) {
				try {
					return performDownload(name,url, destination, context, monitor);
				} catch (ProtocolException e) {
					if (e == ERROR_401)
						context = getConnectionContext(url, true);
				}
			}
		} catch (UserCancelledException e) {
			return Status.CANCEL_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		} finally {
			try {
				destination.close();
			} catch (IOException e) {
				//ignore secondary failure
			}
		}
		//reached maximum number of retries without success
		return new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID,  "IO error", null);
	}

	public IStatus performDownload(String name,String toDownload, OutputStream target, IConnectContext context, IProgressMonitor monitor) throws ProtocolException {
		IRetrieveFileTransferFactory factory = (IRetrieveFileTransferFactory) retrievalFactoryTracker.getService();
		if (factory == null)
			return new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID, "IO error");

		return transfer(name,factory.newInstance(), toDownload, target, context, monitor);
	}

	private IStatus transfer(final String name,final IRetrieveFileTransferContainerAdapter retrievalContainer, final String toDownload, final OutputStream target, IConnectContext context, final IProgressMonitor monitor) throws ProtocolException {
		final IStatus[] result = new IStatus[1];
		IFileTransferListener listener = new IFileTransferListener() {
			private long transferStartTime;
			protected int oldWorked;

			public void handleTransferEvent(IFileTransferEvent event) {
				if (event instanceof IIncomingFileTransferReceiveStartEvent) {
					IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
					try {
						if (target != null) {
							rse.receive(target);
							transferStartTime = System.currentTimeMillis();
						}
						if (monitor != null) {
							long fileLength = rse.getSource().getFileLength();
							final long totalWork = ((fileLength == -1) ? 100 : fileLength);
							int work = (totalWork > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) totalWork;
							monitor.beginTask("Downloading " + name,work);
							oldWorked=0;
						}
					} catch (IOException e) {
						IStatus status = convertToStatus(e);
						synchronized (result) {
							result[0] = status;
							result.notify();
						}
					}
				}
				if (event instanceof IIncomingFileTransferReceiveDataEvent) {
					IIncomingFileTransfer source = ((IIncomingFileTransferReceiveDataEvent) event).getSource();
					if (monitor != null) {
						if (monitor.isCanceled()) {
							source.cancel();
							return;
						}
						long fileLength = source.getFileLength();
						final long totalWork = ((fileLength == -1) ? 100 : fileLength);
						double factor = (totalWork > Integer.MAX_VALUE) ? (((double) Integer.MAX_VALUE) / ((double) totalWork)) : 1.0;
						long received = source.getBytesReceived();
						int worked = (int) Math.round(factor * received);
						double downloadRateBytesPerSecond = (received / ((System.currentTimeMillis() + 1 - transferStartTime) / 1000.0));
						
						String rates = String.format(" (at %s/s)", AbstractRetrieveFileTransfer.toHumanReadableBytes(downloadRateBytesPerSecond));
						String receivedString = AbstractRetrieveFileTransfer.toHumanReadableBytes(received);
						String fileLengthString = AbstractRetrieveFileTransfer.toHumanReadableBytes(fileLength);
						monitor.subTask(receivedString + " of " + fileLengthString + rates);
						monitor.worked(worked-oldWorked);
						oldWorked=worked;
					}
				}
				if (event instanceof IIncomingFileTransferReceiveDoneEvent) {
					Exception exception = ((IIncomingFileTransferReceiveDoneEvent) event).getException();
					IStatus status = convertToStatus(exception);
					synchronized (result) {
						result[0] = status;
						result.notify();
					}
				}
			}
		};

		try {
			retrievalContainer.setConnectContextForAuthentication(context);
			retrievalContainer.sendRetrieveRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getRetrieveNamespace(), toDownload), listener, null);
		} catch (IncomingFileTransferException e) {
			IStatus status = e.getStatus();
			Throwable exception = status.getException();
			if (exception instanceof IOException) {
				if (exception.getMessage() != null && (exception.getMessage().indexOf("401") != -1 || exception.getMessage().indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
					throw ERROR_401;
			}
			return status;
		} catch (FileCreateException e) {
			return e.getStatus();
		}
		waitFor(toDownload, result);
		return result[0];
	}

	private IRemoteFile checkFile(final IRemoteFileSystemBrowserContainerAdapter retrievalContainer, final String location, IConnectContext context) throws ProtocolException {
		final Object[] result = new Object[2];
		final Object FAIL = new Object();
		IRemoteFileSystemListener listener = new IRemoteFileSystemListener() {
			public void handleRemoteFileEvent(IRemoteFileSystemEvent event) {
				Exception exception = event.getException();
				if (exception != null) {
					synchronized (result) {
						result[0] = FAIL;
						result[1] = exception;
						result.notify();
					}
				} else if (event instanceof IRemoteFileSystemBrowseEvent) {
					IRemoteFileSystemBrowseEvent fsbe = (IRemoteFileSystemBrowseEvent) event;
					IRemoteFile[] remoteFiles = fsbe.getRemoteFiles();
					if (remoteFiles != null && remoteFiles.length > 0 && remoteFiles[0] != null) {
						synchronized (result) {
							result[0] = remoteFiles[0];
							result.notify();
						}
					} else {
						synchronized (result) {
							result[0] = FAIL;
							result.notify();
						}
					}
				}
			}
		};
		try {
			retrievalContainer.setConnectContextForAuthentication(context);
			retrievalContainer.sendBrowseRequest(FileIDFactory.getDefault().createFileID(retrievalContainer.getBrowseNamespace(), location), listener);
		} catch (RemoteFileSystemException e) {
			return null;
		} catch (FileCreateException e) {
			return null;
		}
		waitFor(location, result);
		if (result[0] == FAIL && result[1] instanceof IOException) {
			IOException ioException = (IOException) result[1];
			//throw a special exception for authentication failure so we know to prompt for username/password
			String message = ioException.getMessage();
			if (message != null && (message.indexOf(" 401 ") != -1 || message.indexOf(SERVER_REDIRECT) != -1)) //$NON-NLS-1$
				throw ERROR_401;
		}
		if (result[0] instanceof IRemoteFile)
			return (IRemoteFile) result[0];
		return null;
	}

	
	/**
	 * Returns the connection context for the given URL. This may prompt the
	 * user for user name and password as required.
	 * 
	 * @param xmlLocation - the file location requiring login details
	 * @param prompt - use <code>true</code> to prompt the user instead of
	 * looking at the secure preference store for login, use <code>false</code>
	 * to only try the secure preference store
	 * @throws UserCancelledException when the user cancels the login prompt 
	 * @throws CoreException if the password cannot be read or saved
	 * @return The connection context
	 */
	public IConnectContext getConnectionContext(String xmlLocation, boolean prompt) throws UserCancelledException, CoreException {
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
		IPath hostLocation = new Path(xmlLocation).removeLastSegments(1);
		String nodeKey;
		try {
			nodeKey = URLEncoder.encode(hostLocation.toString(), "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e2) {
			//fall back to default platform encoding
			nodeKey = URLEncoder.encode(hostLocation.toString());
		}
		String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;
		ISecurePreferences prefNode = null;
		if (securePreferences.nodeExists(nodeName))
			prefNode = securePreferences.node(nodeName);
		if (!prompt) {
			if (prefNode == null)
				return null;
			try {
				String username = prefNode.get(IRepository.PROP_USERNAME, null);
				String password = prefNode.get(IRepository.PROP_PASSWORD, null);
				//if we don't have stored connection data just return a null connection context
				if (username == null || password == null)
					return null;
				return ConnectContextFactory.createUsernamePasswordConnectContext(username, password);
			} catch (StorageException e) {
				String msg = "Internal Error";
				throw new CoreException(new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID, msg, e));
			}
		}
		//need to prompt user for user name and password
		ServiceTracker adminUITracker = new ServiceTracker(ProjectExamplesActivator.getBundleContext(), IServiceUI.class.getName(), null);
		adminUITracker.open();
		IServiceUI adminUIService = (IServiceUI) adminUITracker.getService();
		AuthenticationInfo loginDetails = null;
		if (adminUIService != null)
			loginDetails = adminUIService.getUsernamePassword(hostLocation.toString());
		//null result means user canceled password dialog
		if (loginDetails == null)
			throw new UserCancelledException();
		//save user name and password if requested by user
		if (loginDetails.saveResult()) {
			if (prefNode == null)
				prefNode = securePreferences.node(nodeName);
			try {
				prefNode.put(IRepository.PROP_USERNAME, loginDetails.getUserName(), true);
				prefNode.put(IRepository.PROP_PASSWORD, loginDetails.getPassword(), true);
				prefNode.flush();
			} catch (StorageException e1) {
				String msg = "Internal error";
				throw new CoreException(new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID,  msg, e1));
			} catch (IOException e) {
				String msg = "Internal error";
				throw new CoreException(new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID, msg, e));
			}
		}
		return ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());
	}

	private IStatus convertToStatus(Exception e) {
		if (e == null)
			return Status.OK_STATUS;
		if (e instanceof UserCancelledException)
			return new Status(IStatus.CANCEL, ProjectExamplesActivator.PLUGIN_ID, e.getMessage(), e);
		return new Status(IStatus.ERROR, ProjectExamplesActivator.PLUGIN_ID, e.getMessage(), e);
	}

	/**
	 * Waits until the first entry in the given array is non-null.
	 */
	private void waitFor(String location, Object[] barrier) {
		WaitJob wait = new WaitJob(location, barrier);
		wait.schedule();
		while (barrier[0] == null) {
			boolean logged = false;
			try {
				wait.join();
			} catch (InterruptedException e) {
				if (!logged)
					LogHelper.log(new Status(IStatus.WARNING, ProjectExamplesActivator.PLUGIN_ID, "Unexpected interrupt while waiting on ECF transfer", e));
			}
		}
	}
}