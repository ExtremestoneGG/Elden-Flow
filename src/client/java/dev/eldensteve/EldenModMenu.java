package dev.eldensteve;
import com.terraformersmc.modmenu.api.*;
public final class EldenModMenu implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory(){return WeaponScreen::new;}
}
