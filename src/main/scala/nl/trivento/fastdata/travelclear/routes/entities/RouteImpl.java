package nl.trivento.fastdata.travelclear.routes.entities;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.model.IdentityBean;

@CsvFields(filename = "routes.txt", prefix = "route_")
public final class RouteImpl extends IdentityBean<Integer> implements Route {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    @CsvField
    private Integer id;

    @CsvField(optional = true, alwaysIncludeInOutput = true)
    private String shortName;

    @CsvField(optional = true, alwaysIncludeInOutput = true)
    private String longName;

    private int type;

    public RouteImpl() {

    }

    public RouteImpl(Integer id, String shortName, String longName, int type) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.type = type;
    }

    public RouteImpl(Integer id) {
        this.id = id;
    }

    public RouteImpl(RouteImpl r) {
        this.id = r.id;
        this.shortName = r.shortName;
        this.longName = r.longName;
        this.type = r.type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "<Route " + id + " " + shortName + ">";
    }
}
