package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTest {

    private SecurityService securityService;
    private PretendDatabaseSecurityRepositoryImpl repository;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private FakeImageService imageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new SecurityService(securityRepository, imageService);
        repository = new PretendDatabaseSecurityRepositoryImpl();
    }

    @AfterEach
    void tearDown() {
        Set<Sensor> sensors = repository.getSensors();
        for (Sensor sensor : new HashSet<>(sensors)) {
            repository.removeSensor(sensor);
        }
        repository.setAlarmStatus(AlarmStatus.NO_ALARM);
        repository.setArmingStatus(ArmingStatus.DISARMED);
    }

    // Test 1
    @Test
    void ifAlarmArmedAndSensorActivated_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @Test
    void ifAlarmArmedAndSensorActivatedAndAlreadyPending_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 3
    @Test
    void ifPendingAlarmAndAllSensorsInactive_returnToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(
                new Sensor("Front Door", SensorType.DOOR),
                new Sensor("Back Door", SensorType.DOOR),
                new Sensor("Mid Door", SensorType.DOOR)
        ));

        securityService.getSensors().forEach(sensor ->
                securityService.changeSensorActivationStatus(sensor, false));

        verify(securityRepository, times(3)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 4
    @Test
    void ifAlarmActiveAndSensorStateChanges_alarmStateUnchanged() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 5
    @Test
    void ifSensorActivatedAndAlreadyActiveAndPending_changeToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 6
    @Test
    void ifSensorDeactivatedAndAlreadyInactive_noChangesToAlarmState() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test 7
    @Test
    void ifImageServiceIdentifiesCatAndSystemArmedHome_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test 8
    @Test
    void ifImageServiceIdentifiesNoCatAndSensorsNotActive_changeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        Set<Sensor> inactiveSensors = new HashSet<>();
        Sensor frontDoor = new Sensor("Front Door", SensorType.DOOR);
        Sensor backDoor = new Sensor("Back Door", SensorType.DOOR);
        frontDoor.setActive(false);
        backDoor.setActive(false);
        inactiveSensors.add(frontDoor);
        inactiveSensors.add(backDoor);

        when(securityRepository.getSensors()).thenReturn(inactiveSensors);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 9
    @Test
    void ifSystemDisarmed_statusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test 10
    @Test
    void ifSystemArmed_resetAllSensorsToInactive() {
        Set<Sensor> sensors = new HashSet<>();
        Sensor frontDoor = new Sensor("Front Door", SensorType.DOOR);
        Sensor backDoor = new Sensor("Back Door", SensorType.DOOR);
        frontDoor.setActive(true);
        backDoor.setActive(true);
        sensors.add(frontDoor);
        sensors.add(backDoor);

        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        sensors.forEach(sensor -> {
            assertFalse(sensor.getActive());
            verify(securityRepository).updateSensor(sensor);
        });
    }

    // Test 11
    @Test
    void ifArmedHomeAndCatDetected_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // For coverage
    @Test
    void testMultipleSensors() {
        Sensor sensor1 = new Sensor("Sensor 1", SensorType.DOOR);
        Sensor sensor2 = new Sensor("Sensor 2", SensorType.WINDOW);
        repository.addSensor(sensor1);
        repository.addSensor(sensor2);
        Set<Sensor> sensors = repository.getSensors();
        assertEquals(2, sensors.size());
        assertTrue(sensors.contains(sensor1));
        assertTrue(sensors.contains(sensor2));
    }
}