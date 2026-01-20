package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;

import com.udacity.catpoint.image.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Core business logic for the security system.
 * All alarm decisions are made here.
 */
public class SecurityService {

    private final SecurityRepository securityRepository;
    private final ImageService imageService;
    private final Set<StatusListener> statusListeners = new HashSet<>();

    // ✅ Track cat detection state
    private boolean catDetected = false;

    public SecurityService(SecurityRepository securityRepository,
                           ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /* -------------------- ARMING -------------------- */

    public void setArmingStatus(ArmingStatus armingStatus) {

        if (armingStatus == ArmingStatus.DISARMED) {
            catDetected = false;
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Reset all sensors when armed
            for (Sensor sensor : securityRepository.getSensors()) {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            }

            // If cat already detected and ARMED_HOME → alarm
            if (armingStatus == ArmingStatus.ARMED_HOME && catDetected) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }

        securityRepository.setArmingStatus(armingStatus);
    }

    /* -------------------- IMAGE PROCESSING -------------------- */

    public void processImage(BufferedImage image) {

        catDetected = imageService.imageContainsCat(image);

        // ✅ FIX: Explicitly set NO_ALARM when system is disarmed
        if (securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
            statusListeners.forEach(sl -> sl.catDetected(catDetected));
            return;
        }

        if (catDetected && securityRepository.getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!catDetected) {

            boolean anySensorActive = securityRepository.getSensors()
                    .stream()
                    .anyMatch(Sensor::getActive);

            if (!anySensorActive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }

        statusListeners.forEach(sl -> sl.catDetected(catDetected));
    }


    /* -------------------- SENSOR HANDLING -------------------- */

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {

        // ❗ If already in ALARM, ignore all sensor changes
        if (securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return;
        }

        if (sensor.getActive() && active) {
            // Already active
            if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
            return;
        }

        if (!sensor.getActive() && !active) {
            return;
        }

        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        if (active) {
            handleSensorActivated();
        } else {
            handleSensorDeactivated();
        }
    }

    private void handleSensorActivated() {

        if (securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }

        switch (securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void handleSensorDeactivated() {

        if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            boolean anyActive = securityRepository.getSensors()
                    .stream()
                    .anyMatch(Sensor::getActive);

            if (!anyActive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
    }

    /* -------------------- ALARM -------------------- */

    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /* -------------------- PASSTHROUGH -------------------- */

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public void addStatusListener(StatusListener listener) {
        statusListeners.add(listener);
    }

    public void removeStatusListener(StatusListener listener) {
        statusListeners.remove(listener);
    }
}
