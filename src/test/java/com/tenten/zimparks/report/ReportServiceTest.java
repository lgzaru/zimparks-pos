package com.tenten.zimparks.report;

import com.tenten.zimparks.creditnote.CreditNoteRepository;
import com.tenten.zimparks.transaction.Transaction;
import com.tenten.zimparks.transaction.TransactionRepository;
import com.tenten.zimparks.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository txRepo;

    @Mock
    private CreditNoteRepository cnRepo;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void build_shouldPaginateCorrectly() {
        // Arrange
        List<Transaction> paidTransactions = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Transaction t = new Transaction();
            t.setRef("TX" + i);
            t.setStatus(TransactionStatus.PAID);
            paidTransactions.add(t);
        }
        
        when(txRepo.findByStatus(TransactionStatus.PAID)).thenReturn(paidTransactions);
        when(txRepo.findAll()).thenReturn(new ArrayList<>());
        when(cnRepo.findAll()).thenReturn(new ArrayList<>());

        Map<String, String> filters = new HashMap<>();
        
        // Page 0, Size 10
        Pageable page0 = PageRequest.of(0, 10);
        Page<Map<String, Object>> result0 = reportService.build("Shift Revenue Summary", filters, page0);
        
        assertEquals(10, result0.getContent().size());
        assertEquals(15, result0.getTotalElements());
        assertEquals("TX0", result0.getContent().get(0).get("Ref"));
        assertEquals("TX9", result0.getContent().get(9).get("Ref"));

        // Page 1, Size 10
        Pageable page1 = PageRequest.of(1, 10);
        Page<Map<String, Object>> result1 = reportService.build("Shift Revenue Summary", filters, page1);
        
        assertEquals(5, result1.getContent().size());
        assertEquals(15, result1.getTotalElements());
        assertEquals("TX10", result1.getContent().get(0).get("Ref"));
        assertEquals("TX14", result1.getContent().get(4).get("Ref"));
    }

    @Test
    void build_shouldReturnEmptyIfPageOutOfBounds() {
        // Arrange
        List<Transaction> paidTransactions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Transaction t = new Transaction();
            t.setRef("TX" + i);
            t.setStatus(TransactionStatus.PAID);
            paidTransactions.add(t);
        }
        
        when(txRepo.findByStatus(TransactionStatus.PAID)).thenReturn(paidTransactions);
        when(txRepo.findAll()).thenReturn(new ArrayList<>());
        when(cnRepo.findAll()).thenReturn(new ArrayList<>());

        Map<String, String> filters = new HashMap<>();
        
        // Page 1, Size 10 (out of bounds for 5 elements)
        Pageable page1 = PageRequest.of(1, 10);
        Page<Map<String, Object>> result1 = reportService.build("Shift Revenue Summary", filters, page1);
        
        assertEquals(0, result1.getContent().size());
        assertEquals(5, result1.getTotalElements());
    }
}
