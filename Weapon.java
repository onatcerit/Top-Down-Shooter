
public class Weapon {
    private String name;
    private int fireRate;
    private int magazineSize;
    private int totalAmmo;
    private int ammoInMagazine;
    private int damagePerShot;

    public Weapon(String name, int fireRate, int magazineSize, int totalAmmo, int damagePerShot) {
        this.name = name;
        this.fireRate = fireRate;
        this.magazineSize = magazineSize;
        this.totalAmmo = totalAmmo;
        this.ammoInMagazine = magazineSize;
        this.damagePerShot = damagePerShot;
    }

    public boolean canShoot() { return ammoInMagazine > 0; }

    public void shoot() {
        if (ammoInMagazine > 0) ammoInMagazine--;
    }

    public void reload() {
        int missing = magazineSize - ammoInMagazine;
        if (totalAmmo >= missing) {
            totalAmmo -= missing;
            ammoInMagazine = magazineSize;
        } 
        else {
            ammoInMagazine += totalAmmo;
            totalAmmo = 0;
        }
    }

    public String getName() {
        return name; 
    }
    public int getMagazineSize() {
        return magazineSize; 
    }
    public int getAmmoInMagazine() {
        return ammoInMagazine; 
    }
    public int getTotalAmmo() {
         return totalAmmo; 
    }
    public int getDamagePerShot() {
        return damagePerShot;
    }
}
