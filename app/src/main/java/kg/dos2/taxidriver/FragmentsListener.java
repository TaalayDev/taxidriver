package kg.dos2.taxidriver;

public interface FragmentsListener {
    void tryConnectToOrder(int id);
    void curFrag(int f);
    void cancOrd();
    void complOrd(int w);
    void finishOrd();
    void startWalking();
    void openClientChat();
    void dialogDismiss();
}
