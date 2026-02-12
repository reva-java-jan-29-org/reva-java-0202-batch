package com.training.campussystem.model.enums;

import java.util.Objects;

public final class Zone {
	private final String zoneId;
	private final String name;
	private final ZoneType zoneType;
	private final int openHour;
	private final int closeHour;

	public Zone(String zoneId, String name, ZoneType zoneType, int openHour, int closeHour) {
		this.zoneId = Objects.requireNonNull(zoneId);
		this.name = Objects.requireNonNull(name);
		this.zoneType = Objects.requireNonNull(zoneType);
		this.openHour = openHour;
		this.closeHour = closeHour;
	}

	public String getZoneId() {
		return zoneId;
	}

	public String getName() {
		return name;
	}

	public ZoneType getZoneType() {
		return zoneType;
	}

	public int getOpenHour() {
		return openHour;
	}

	public int getCloseHour() {
		return closeHour;
	}
}
