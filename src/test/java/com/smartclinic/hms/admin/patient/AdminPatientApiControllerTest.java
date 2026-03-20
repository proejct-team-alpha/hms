package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.UpdateAdminPatientApiResponse;
import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPatientApiController.class)
@Import(AdminControllerTestSecurityConfig.class)
class AdminPatientApiControllerTest {

    private static final String UPDATE_SUCCESS_MESSAGE = "환자 정보가 수정되었습니다.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPatientService adminPatientService;

    @Test
    @DisplayName("admin can update patient via admin api")
    void updatePatient_success() throws Exception {
        // given
        UpdateAdminPatientApiResponse response = new UpdateAdminPatientApiResponse(
                10L,
                "김수정",
                "010-9999-8888",
                "메모 수정",
                UPDATE_SUCCESS_MESSAGE
        );
        given(adminPatientService.updatePatient(10L, "김수정", "010-9999-8888", "메모 수정"))
                .willReturn(response);

        // when // then
        mockMvc.perform(post("/admin/api/patients/10/update")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "김수정",
                                  "phone": "010-9999-8888",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.body.patientId").value(10))
                .andExpect(jsonPath("$.body.name").value("김수정"))
                .andExpect(jsonPath("$.body.phone").value("010-9999-8888"))
                .andExpect(jsonPath("$.body.note").value("메모 수정"))
                .andExpect(jsonPath("$.body.message").value(UPDATE_SUCCESS_MESSAGE));

        then(adminPatientService).should().updatePatient(10L, "김수정", "010-9999-8888", "메모 수정");
    }

    @Test
    @DisplayName("non-admin cannot update patient via admin api")
    void updatePatient_forbiddenWhenNotAdmin() throws Exception {
        // given

        // when // then
        mockMvc.perform(post("/admin/api/patients/10/update")
                        .with(user("staff").roles("STAFF"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "김수정",
                                  "phone": "010-9999-8888",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminPatientService);
    }

    @Test
    @DisplayName("returns 400 when name is blank")
    void updatePatient_validationFailureWhenNameIsBlank() throws Exception {
        // given

        // when // then
        mockMvc.perform(post("/admin/api/patients/10/update")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "phone": "010-9999-8888",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.msg", containsString("name")));

        verifyNoInteractions(adminPatientService);
    }

    @Test
    @DisplayName("returns 400 when phone is blank")
    void updatePatient_validationFailureWhenPhoneIsBlank() throws Exception {
        // given

        // when // then
        mockMvc.perform(post("/admin/api/patients/10/update")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "김수정",
                                  "phone": "",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.msg", containsString("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.msg", containsString("phone")));

        verifyNoInteractions(adminPatientService);
    }

    @Test
    @DisplayName("returns 409 when phone is duplicated by another patient")
    void updatePatient_conflictWhenPhoneDuplicated() throws Exception {
        // given
        given(adminPatientService.updatePatient(eq(10L), eq("김수정"), eq("010-9999-8888"), eq("메모 수정")))
                .willThrow(CustomException.conflict("DUPLICATE_PATIENT_PHONE", "이미 사용 중인 연락처입니다."));

        // when // then
        mockMvc.perform(post("/admin/api/patients/10/update")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "김수정",
                                  "phone": "010-9999-8888",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.msg", containsString("DUPLICATE_PATIENT_PHONE")))
                .andExpect(jsonPath("$.msg", containsString("이미 사용 중인 연락처입니다.")));
    }

    @Test
    @DisplayName("returns 404 when patient does not exist")
    void updatePatient_notFound() throws Exception {
        // given
        given(adminPatientService.updatePatient(eq(999L), eq("김수정"), eq("010-9999-8888"), eq("메모 수정")))
                .willThrow(CustomException.notFound("환자를 찾을 수 없습니다."));

        // when // then
        mockMvc.perform(post("/admin/api/patients/999/update")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "김수정",
                                  "phone": "010-9999-8888",
                                  "note": "메모 수정"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.msg", containsString("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.msg", containsString("환자를 찾을 수 없습니다.")));
    }
}
