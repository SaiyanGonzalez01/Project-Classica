package net.lax1dude.eaglercraft.internal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.websocket.WebSocket;

import net.lax1dude.eaglercraft.internal.teavm.TeaVMServerQuery;
import net.lax1dude.eaglercraft.internal.teavm.TeaVMUtils;

/**
 * Copyright (c) 2022-2023 lax1dude, ayunami2000. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
public class PlatformNetworking {
	
	private static WebSocket sock = null;
	private static boolean sockIsConnecting = false;
	private static boolean sockIsConnected = false;
	private static boolean sockIsAlive = false;
	private static boolean sockIsFailed = false;
	private static LinkedList<byte[]> readPackets = new LinkedList();
	private static String currentSockURI = null;
	private static EnumServerRateLimit serverRateLimit = null;
	
	private static final Logger logger = LogManager.getLogger("PlatformNetworking");
	
	public static EnumEaglerConnectionState playConnectionState() {
		return !sockIsConnected ? (sockIsFailed ? EnumEaglerConnectionState.FAILED : EnumEaglerConnectionState.CLOSED)
				: (sockIsConnecting ? EnumEaglerConnectionState.CONNECTING : EnumEaglerConnectionState.CONNECTED);
	}

	public static void startPlayConnection(String destination) {
		sockIsFailed = !connectWebSocket(destination).booleanValue();
	}

	@JSBody(params = { "obj" }, script = "return typeof obj === \"string\";")
	private static native boolean isString(JSObject obj);

	@Async
	public static native Boolean connectWebSocket(String sockURI);
	
	private static void connectWebSocket(String sockURI, final AsyncCallback<Boolean> cb) {
		sockIsConnecting = true;
		sockIsConnected = false;
		sockIsAlive = false;
		currentSockURI = sockURI;
		try {
			sock = WebSocket.create(sockURI);
		} catch(Throwable t) {
			sockIsFailed = true;
			sockIsConnecting = false;
			sockIsAlive = false;
			cb.complete(Boolean.FALSE);
			return;
		}
		final WebSocket oldSock = sock;
		sock.setBinaryType("arraybuffer");
		TeaVMUtils.addEventListener(sock, "open", new EventListener<Event>() {
			@Override
			public void handleEvent(Event evt) {
				if (oldSock != sock) return;
				sockIsConnecting = false;
				sockIsAlive = false;
				sockIsConnected = true;
				synchronized(readPackets) {
					readPackets.clear();
				}
				cb.complete(Boolean.TRUE);
			}
		});
		TeaVMUtils.addEventListener(sock, "close", new EventListener<Event>() {
			@Override
			public void handleEvent(Event evt) {
				if (oldSock != sock) return;
				sock = null;
				boolean b = sockIsConnecting;
				sockIsConnecting = false;
				sockIsConnected = false;
				sockIsAlive = false;
				if(b) cb.complete(Boolean.FALSE);
			}
		});
		TeaVMUtils.addEventListener(sock, "message", new EventListener<MessageEvent>() {
			@Override
			public void handleEvent(MessageEvent evt) {
				if (oldSock != sock) return;
				sockIsAlive = true;
				if(isString(evt.getData())) {
					String str = evt.getDataAsString();
					if(str.equalsIgnoreCase("BLOCKED")) {
						logger.error("Reached full IP ratelimit!");
						serverRateLimit = EnumServerRateLimit.BLOCKED;
					}else if(str.equalsIgnoreCase("LOCKED")) {
						logger.error("Reached full IP ratelimit lockout!");
						serverRateLimit = EnumServerRateLimit.LOCKED_OUT;
					}
				}else {
					synchronized(readPackets) {
						readPackets.add(TeaVMUtils.wrapByteArrayBuffer(evt.getDataAsArray()));
					}
				}
			}
		});
		TeaVMUtils.addEventListener(sock, "error", new EventListener<Event>() {
			@Override
			public void handleEvent(Event evt) {
				if (oldSock != sock) return;
				if(sockIsConnecting) {
					sockIsFailed = true;
					sockIsConnecting = false;
					sockIsAlive = false;
					cb.complete(Boolean.FALSE);
				}
			}
		});
	}

	public static void playDisconnect() {
		if(sock != null) sock.close();
		sockIsConnecting = false;
	}

	public static byte[] readPlayPacket() {
		synchronized(readPackets) {
			if(!readPackets.isEmpty()) {
				return readPackets.remove(0);
			}else {
				return null;
			}
		}
	}

	public static List<byte[]> readAllPacket() {
		synchronized(readPackets) {
			if(!readPackets.isEmpty()) {
				List<byte[]> ret = new ArrayList<>(readPackets);
				readPackets.clear();
				return ret;
			}else {
				return null;
			}
		}
	}

	public static int countAvailableReadData() {
		int total = 0;
		synchronized(readPackets) {
			for(int i = 0, l = readPackets.size(); i < l; ++i) {
				total += readPackets.get(i).length;
			}
		}
		return total;
	}

	@JSBody(params = { "sock", "buffer" }, script = "sock.send(buffer);")
	protected static native void nativeBinarySend(WebSocket sock, ArrayBuffer buffer);

	public static void writePlayPacket(byte[] pkt) {
		if(sock != null && !sockIsConnecting) {
			nativeBinarySend(sock, TeaVMUtils.unwrapArrayBuffer(pkt));
		}
	}

	public static IServerQuery sendServerQuery(String uri, String accept) {
		try {
			return new TeaVMServerQuery(uri, accept);
		}catch(Throwable t) {
			logger.error("Could not send query to \"{}\"!", uri);
			logger.error(t);
			return null;
		}
	}

	public static EnumServerRateLimit getRateLimit() {
		return serverRateLimit == null ? EnumServerRateLimit.OK : serverRateLimit;
	}

	public static String getCurrentURI() {
		return currentSockURI;
	}
}
