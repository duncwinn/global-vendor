package gv.warehouse.api;

public interface StockAlertEventSource {
	
	public void setStockAlertListener(StockAlertListener listener);

	public void setStockAlertThreshold(int threshold);
}
