// Holden Ernest - 10/10/2024

// This Response is a structure which is passed to any method that might have an addition to the
// eventual response which is sent back to the connector.

public class Response {
    private String data;
    private int status = 200;
    private int version = -1;

    Response() {
        //do nothing
    }

    public void setStatus(int s) {
        status = s;
    }
    public int getStatus() {
        return status;
    }
    public String getData() {
        return data;
    }
    public void setData(String d) {
        data = d;
    }
    public void setVersion(int v) {
        version = v;
    }
    public int getVersion() {
        return version;
    }
}
