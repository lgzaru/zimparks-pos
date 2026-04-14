package com.tenten.zimparks.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResult {

    private List<Product> imported;
    private List<RowError> failed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private String descr;
        private String stationId;
        private String error;
    }
}
