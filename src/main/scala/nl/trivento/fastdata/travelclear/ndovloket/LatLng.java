package nl.trivento.fastdata.travelclear.ndovloket;

public class LatLng {
    private static double referenceWgs84X = 52.15517;
    private static double referenceWgs84Y = 5.387206;
    private static int referenceRdX = 155000;
    private static int referenceRdY = 463000;

    private final double latitude;
    private final double longitude;

    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static LatLng fromRijksdriehoek(int x, int y) {
        double dX = 1E-5 * (x - 155E3);
        double dY = 1E-5 * (y - 463E3);

        double dXp2 = Math.pow(dX, 2);
        double dYp2 = Math.pow(dY, 2);
        double dYp3 = Math.pow(dY, 3);
        double dXp4 = Math.pow(dX, 4);
        double dXp3 = Math.pow(dX, 3);
        double dYp4 = Math.pow(dY, 4);
        double dXp5 = Math.pow(dX, 5);

        double lat = 52.1551744 +
                (1 * dY * 3235.65389 +
                dXp2 * -32.58297 +
                dYp2 * -0.2475 +
                dXp2 * dY * -0.84978 +
                dYp3 * -0.0665 +
                dXp2 * dYp2 * -0.01709 +
                dX * -0.00738 +
                dXp4 * 0.0053 +
                dXp2 * dYp3 * -3.9E-4 +
                dXp4 * dY * 3.3E-4 +
                dX * dY * -1.2E-4) / 3600;

        double lng = 5.38720621 +
                (dX * 5260.52916 +
                dX * dY * 105.94684 +
                dX * dYp2 * 2.45656 +
                dXp2 * dX * -0.81885 +
                dX * dYp3 * 0.05594 +
                dXp3 * dY * -0.05607 +
                dY * 0.01199 +
                dXp3 * dYp2 * -0.00256 +
                dX * dYp4 * 0.00128 +
                dYp2 * 2.2E-4 +
                dXp2 * -2.2E-4 +
                dXp5 * 2.6E-4) / 3600;

        return new LatLng(lat, lng);
    }

    public Position toPosition(String name) {
        return new Position(name, getLatitude(), getLongitude());
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
