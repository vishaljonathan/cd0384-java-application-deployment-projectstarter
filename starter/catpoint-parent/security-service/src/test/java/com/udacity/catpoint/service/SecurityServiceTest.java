package com.udacity.catpoint.service;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.data.SensorType;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setup() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // -------------------------------------------------
    // SENSOR / ALARM BEHAVIOR TESTS
    // -------------------------------------------------

    @Test
    void armedSystem_sensorActivated_setsPendingAlarm() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void pendingAlarm_sensorActivated_setsAlarm() {
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void pendingAlarm_allSensorsInactive_setsNoAlarm() {
        Sensor sensor = new Sensor("Door", SensorType.DOOR);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmActive_sensorChange_doesNotChangeAlarm() {
        Sensor sensor = new Sensor("Motion", SensorType.MOTION);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never())
                .setAlarmStatus(any());
    }

    @Test
    void sensorAlreadyActive_pendingAlarm_setsAlarm() {
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void sensorAlreadyInactive_doesNothing() {
        Sensor sensor = new Sensor("Door", SensorType.DOOR);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never())
                .setAlarmStatus(any());
    }

    // -------------------------------------------------
    // IMAGE SERVICE / CAMERA TESTS
    // -------------------------------------------------

    @Test
    void armedHome_catDetected_setsAlarm() {
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any()))
                .thenReturn(true);

        securityService.processImage(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        );

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatDetected_noActiveSensors_setsNoAlarm() {
        when(imageService.imageContainsCat(any()))
                .thenReturn(false);
        when(securityRepository.getSensors())
                .thenReturn(Set.of());

        securityService.processImage(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        );

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void catDetected_systemDisarmed_setsNoAlarm() {
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any()))
                .thenReturn(true);

        securityService.processImage(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        );

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // -------------------------------------------------
    // ARMING / DISARMING BEHAVIOR TESTS
    // -------------------------------------------------

    @Test
    void disarmingSystem_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void armingSystem_resetsAllSensorsToInactive() {
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        sensor.setActive(true);

        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor));

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        assertFalse(sensor.getActive());
    }

    @Test
    void armedHome_catPreviouslyDetected_setsAlarm() {
        when(imageService.imageContainsCat(any()))
                .thenReturn(true);
        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        );

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}
