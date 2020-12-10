package kg.dos2.taxidriver;

public class Order {
    public static Order current = null;

    private int id = 0;
    private int state = 0;
    private int cat = 7;
    private int cost = 0;
    private int tp = 0;
    private int clientId = 0;
    private int wmin = 0;
    private float dist = 0;
    private float lat1 = 0;
    private float lat2 = 0;
    private float lng1 = 0;
    private float lng2 = 0;
    private String addr1 = "";
    private String addr2 = "";
    private String phone = "0";
    private String desc = "";
    private String date = "";

    Order() { }

    Order(int id, int cat, int cost, float dist, float lat1, float lng1, float lat2, float lng2,
          String addr1, String addr2, String phone, String desc, String date, int clientId)
    {
        this.id = id;
        this.cat = cat;
        this.cost = cost;
        this.dist = dist;
        this.lat1 = lat1;
        this.lng1 = lng1;
        this.lat2 = lat2;
        this.lng2 = lng2;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.phone = phone;
        this.desc = desc;
        this.clientId = clientId;
        this.date = date;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTp(int tp) {
        this.tp = tp;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setCat(int cat) {
        this.cat = cat;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setDist(float dist) {
        this.dist = dist;
    }

    public void setLat1(float lat1) {
        this.lat1 = lat1;
    }

    public void setLat2(float lat2) {
        this.lat2 = lat2;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setLng1(float lng1) {
        this.lng1 = lng1;
    }

    public void setLng2(float lng2) {
        this.lng2 = lng2;
    }

    public void setAddr1(String addr1) {
        this.addr1 = addr1;
    }

    public void setAddr2(String addr2) {
        this.addr2 = addr2;
    }

    public void setWmin(int wmin) {
        this.wmin = wmin;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getDist() {
        return dist;
    }

    public int getClientId() {
        return clientId;
    }

    public int getState() {
        return state;
    }

    public int getTp() {
        return tp;
    }

    public float getLat1() {
        return lat1;
    }

    public float getLat2() {
        return lat2;
    }

    public int getCat() {
        return cat;
    }

    public int getCost() {
        return cost;
    }

    public int getId() {
        return id;
    }

    public float getLng1() {
        return lng1;
    }

    public float getLng2() {
        return lng2;
    }

    public String getAddr1() {
        return addr1;
    }

    public String getAddr2() {
        return addr2;
    }

    public String getDesc() {
        return desc;
    }

    public String getPhone() {
        return phone;
    }

    public int getWmin() {
        return wmin;
    }

    public String getDate() {
        return date;
    }
}