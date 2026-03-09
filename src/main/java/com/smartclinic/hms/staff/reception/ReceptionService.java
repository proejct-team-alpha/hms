package com.smartclinic.hms.staff.reception;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReceptionService {

    @Transactional
    public void receive(ReceptionUpdateRequest request) {

    }

}
