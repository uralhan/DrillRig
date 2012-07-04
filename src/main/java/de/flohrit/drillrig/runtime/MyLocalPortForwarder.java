package de.flohrit.drillrig.runtime;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import net.schmizz.concurrent.Event;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SSHPacket;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.Connection;
import net.schmizz.sshj.connection.channel.SocketStreamCopyMonitor;
import net.schmizz.sshj.connection.channel.direct.AbstractDirectChannel;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder.Parameters;
import net.schmizz.sshj.transport.TransportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flohrit.drillrig.config.Forward;

public 	class MyLocalPortForwarder extends Thread implements PortForwarder {
	final static private Logger logger = LoggerFactory
			.getLogger(MyLocalPortForwarder.class);
	
	private SSHClient client;
	private Parameters parameters;
	private ServerSocket serverSocket;

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Forcing socket close failed.");
		}
	}

	MyLocalPortForwarder(SSHClient client,
			Forward forward) throws IOException {
		super("LForwarder-" + forward.getSPort());
		this.client = client;
		
		serverSocket = new java.net.ServerSocket(
					forward.getSPort(), 50,
					InetAddress.getByName("*".equals(forward
							.getSHost()) ? "0.0.0.0" : forward
							.getSHost()));


		Parameters parameters = new LocalPortForwarder.Parameters(
				forward.getSHost(), forward.getSPort(),
				forward.getRHost(), forward.getRPort());
									
		this.parameters = parameters;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				startForwarding(socket);
			} catch (ConnectException e) { // i.e. connection refused on
											// remote end

			} catch (TransportException e) {
				logger.warn(
						"Local listener for port {} terminated with reason: {}",
						new Object[] {
								serverSocket
										.getLocalPort(),
								e.getDisconnectReason() });

				break;
			} catch (IOException e) {
				logger.warn(
						"Local listener for port {} terminated with IOException",
						new Object[] {
								serverSocket
										.getLocalPort(),
								e.getLocalizedMessage() });
				break;
			}
		}
	}

	void startForwarding(Socket socket) throws IOException {
		final DirectTCPIPChannel chan = new DirectTCPIPChannel(
				client.getConnection(), socket, parameters);
		chan.open();
		chan.start();
	}

	class DirectTCPIPChannel extends AbstractDirectChannel {

		protected final Socket socket;
		protected final Parameters parameters;

		DirectTCPIPChannel(Connection conn, Socket socket,
				Parameters parameters) {
			super(conn, "direct-tcpip");
			this.socket = socket;
			this.parameters = parameters;
		}

		protected void start() throws IOException {
			socket.setSendBufferSize(getLocalMaxPacketSize());
			socket.setReceiveBufferSize(getRemoteMaxPacketSize());
			final Event<IOException> soc2chan = new StreamCopier(
					socket.getInputStream(), getOutputStream()).bufSize(
					getRemoteMaxPacketSize()).spawnDaemon("soc2chan");
			final Event<IOException> chan2soc = new StreamCopier(
					getInputStream(), socket.getOutputStream()).bufSize(
					getLocalMaxPacketSize()).spawnDaemon("chan2soc");
			SocketStreamCopyMonitor.monitor(5, TimeUnit.SECONDS, soc2chan,
					chan2soc, this, socket);
		}

		@Override
		protected SSHPacket buildOpenReq() {
			return super.buildOpenReq()
					.putString(parameters.getRemoteHost())
					.putUInt32(parameters.getRemotePort())
					.putString(parameters.getLocalHost())
					.putUInt32(parameters.getLocalPort());
		}
	}
}
