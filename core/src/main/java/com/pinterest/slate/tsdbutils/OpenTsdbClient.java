package com.pinterest.slate.tsdbutils;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

/**
 * A client library for sending metrics to an OpenTSDB server.
 * 
 * The API for sending data to OpenTSDB is documented at:
 *
 *  http://opentsdb.net/docs/build/html/user_guide/writing.html
 *
 *  put <metric> <timestamp> <value> <tagk1=tagv1[ tagk2=tagv2 ...tagkN=tagvN]>
 *
 */
public class OpenTsdbClient {
  private static final Logger LOG = LoggerFactory.getLogger(OpenTsdbClient.class);
  private static final int CONNECT_TIMEOUT_MS = 3000;
  private final SocketAddress address;
  private Socket socket;

  public static final class MetricsBuffer {

    public MetricsBuffer() {
      this.buffer = new StringBuilder();
    }

    /**
     * Add a single metric to the buffer.
     *
     * @param name the name of the metric, like "foo.bar.sprockets".
     * @param epochSecs the UNIX epoch time in seconds.
     * @param value the value of the metric at this epoch time.
     * @param tags a list of one or more tags, each of which must be formatted as "name=value".
     */
    public void addMetric(String name, int epochSecs, float value, String... tags) {
      addMetric(name, epochSecs, value, SPACE_JOINER.join(tags));
    }

    public void addMetric(String name, int epochSecs, float value, String tags) {
      buffer.append("put ")
          .append(name)
          .append(" ")
          .append(epochSecs)
          .append(" ")
          .append(value)
          .append(" ")
          .append(tags)
          .append("\n");
    }

    /**
     * Reset the metrics buffer for reuse, this discards all previous data.
     */
    public void reset() {
      buffer.setLength(0);
    }

    @Override
    public String toString() {
      return buffer.toString();
    }

    private final StringBuilder buffer;
    private static final Joiner SPACE_JOINER = Joiner.on(" ");
  }

  public static class OpenTsdbClientException extends Exception {

    private static final long serialVersionUID = 1L;

    public OpenTsdbClientException(Throwable causedBy) {
      super(causedBy);
    }
  }

  public static final class ConnectionFailedException extends OpenTsdbClientException {

    private static final long serialVersionUID = 1L;

    public ConnectionFailedException(Throwable causedBy) {
      super(causedBy);
    }
  }

  public static final class SendFailedException extends OpenTsdbClientException {

    private static final long serialVersionUID = 1L;

    public SendFailedException(Throwable causedBy) {
      super(causedBy);
    }
  }

  public OpenTsdbClient(String host, int port) throws UnknownHostException, ConnectionFailedException {
    InetAddress address = InetAddress.getByName(host);
    this.address = new InetSocketAddress(address, port);
  }
  
  public void connect() throws ConnectionFailedException {
    try {
      socket = new Socket();
      socket.connect(address, CONNECT_TIMEOUT_MS);
    } catch (IOException ioex) {
      throw new ConnectionFailedException(ioex);
    }
  }

  public void sendMetrics(MetricsBuffer buffer)
      throws ConnectionFailedException, SendFailedException {
    if (socket == null) {
      connect();
    }
    try {
      try {
        // There is no way to set a time out for blocking send calls. Thanks Java!
        socket.getOutputStream().write(buffer.toString().getBytes());
      } catch (IOException ioex) {
        // nullify the socket object so we try to reconnect in next iteration
        socket = null;
        throw new SendFailedException(ioex);
      }
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ioex) {
          LOG.warn("Failed to close socket to OpenTSDB", ioex);
        }
      }
    }
  }
}
