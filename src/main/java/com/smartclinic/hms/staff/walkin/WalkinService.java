package com.smartclinic.hms.staff.walkin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WalkinService {

    public void createWalkin() {

        // TODO: 방문 접수 생성 로직

        System.out.println("walkin reception created");
    }
}
