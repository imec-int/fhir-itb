package eu.europa.ec.fhir.pseudo;

class PseudoInTransitDto {
    private String x;
    private String y;
    private String transitInfo;
    private Long exp;
    private Long iat;
    private String crv;

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getTransitInfo() {
        return transitInfo;
    }

    public void setTransitInfo(String transitInfo) {
        this.transitInfo = transitInfo;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public String getCrv() {
        return crv;
    }

    public void setCrv(String crv) {
        this.crv = crv;
    }
}
