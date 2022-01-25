package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/15/15.
 */
public class Config {

    public List<Product> products;
    public List<Region> regions;

    public List<Product> getProducts() {
        return products;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }

}
