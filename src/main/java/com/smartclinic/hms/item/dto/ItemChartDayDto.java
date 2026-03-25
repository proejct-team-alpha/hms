package com.smartclinic.hms.item.dto;

public class ItemChartDayDto {

    private final String label;   // "03/17"
    private final int inAmount;
    private final int outAmount;
    private final int inHeight;   // 0~100 (%)
    private final int outHeight;

    public ItemChartDayDto(String label, int inAmount, int outAmount, int inHeight, int outHeight) {
        this.label = label;
        this.inAmount = inAmount;
        this.outAmount = outAmount;
        this.inHeight = inHeight;
        this.outHeight = outHeight;
    }

    public String getLabel() { return label; }
    public int getInAmount() { return inAmount; }
    public int getOutAmount() { return outAmount; }
    public int getInHeight() { return inHeight; }
    public int getOutHeight() { return outHeight; }
}
