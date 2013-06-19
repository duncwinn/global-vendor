package gv.jpa.service;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import gv.api.Product;
import gv.api.Shipment;
import gv.api.ShipmentLine;
import gv.api.Warehouse;
import gv.warehouse.api.ShipmentConfirmation;
import gv.warehouse.api.ShipmentRequest;
import gv.warehouse.api.StockAlert;
import gv.warehouse.api.StockAlertListener;
import gv.warehouse.api.StockChangeRequest;
import gv.warehouse.api.StockQueryRequest;
import gv.warehouse.jpa.service.WarehouseServiceImpl;
import gv.warehouse.jpa.service.entity.StockLevel;
import gv.warehouse.jpa.service.repository.StockLevelRepository;

@RunWith(MockitoJUnitRunner.class)
public class WarehouseServiceImplTest {
	
	private WarehouseServiceImpl service;
	
	@Mock
	private StockLevelRepository repository;
	
	class Captor implements StockAlertListener {
		
		private StockAlert alert;

		@Override
		public void handleStockAlert(StockAlert alert) {
			this.alert = alert;
		}
		
		public StockAlert getAlert() {
			return alert;
		}
		
		public void clear() {
			alert = null;
		}
	}
	
	private Captor listener = new Captor();
	
	@Before
	public void setUp() {
		service = new WarehouseServiceImpl(repository);
		service.setStockAlertListener(listener);
		service.setStockAlertThreshold(5);
	}
	
	@Test
	public void shouldUpdateStockLevelForNonExistentProduct() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int stockDelta = 26;
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(null);
		
		// when
		int newStockLevel = service.updateStock(new StockChangeRequest(warehouseId, productId, stockDelta));
		
		// then
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		verify(repository).save(any(StockLevel.class));
		assertEquals(stockDelta, newStockLevel);
		
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(stockDelta, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}
	
	@Test
	public void shouldUpdateStockLevelForExistingProduct() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int currentStock = 3;
		int stockDelta = 26;
		StockLevel existingStock = new StockLevel(warehouseId, productId, currentStock);
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStock);
		
		// when
		int newStockLevel = service.updateStock(new StockChangeRequest(warehouseId, productId, stockDelta));
		
		// then
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		verify(repository).save(any(StockLevel.class));
		assertEquals(currentStock + stockDelta, newStockLevel);
		
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(newStockLevel, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}
	
	@Test 
	public void shouldSetStockLevelForNonExistentProduct() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int qty = 26;
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(null);
		
		// when
		service.setStock(new StockChangeRequest(warehouseId, productId, qty));
		
		// then
		StockLevel expectedNewStockLevel = new StockLevel(warehouseId, productId, qty);
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		verify(repository).save(eq(expectedNewStockLevel));
		
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(qty, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}

	@Test 
	public void shouldSetStockLevelForExistingProduct() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int qty = 26;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, 2);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		// when
		service.setStock(new StockChangeRequest(warehouseId, productId, qty));
		
		// then
		StockLevel expectedNewStockLevel = new StockLevel(warehouseId, productId, qty);
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		verify(repository).save(eq(expectedNewStockLevel));
		
		
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(qty, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}
	
	@Test
	public void shouldReturnZeroStockForNonExistentProduct() {
		// given 
		Long productId = 3L;
		Long warehouseId = 5L;
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(null);
		
		// when
		int stockLevel = service.getStock(new StockQueryRequest(warehouseId, productId));
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		assertEquals(0,  stockLevel);
	}

	@Test
	public void shouldReturnCurrentStockForExistingProduct() {
		// given 
		Long productId = 3L;
		Long warehouseId = 5L;
		int qty = 26;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, qty);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		// when
		int stockLevel = service.getStock(new StockQueryRequest(warehouseId, productId));
		verify(repository).findByWarehouseIdAndProductId(warehouseId, productId);
		assertEquals(qty,  stockLevel);
	}
	
	@Test 
	public void shouldConfirmShipmentRequestAndNotTriggerAnAlert() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int qty = 3;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, 15);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		ShipmentRequest request = new ShipmentRequest(warehouseId, productId, qty);
		
		// when
		ShipmentConfirmation confirmation = service.requestShipment(request);
		
		// then
		assertNotNull(confirmation);
		assertEquals(qty, confirmation.getQty());
		assertNull(listener.getAlert());
	}
	

	@Test 
	public void shouldConfirmShipmentRequestAndTriggerAnAlert() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		int qty = 12;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, 15);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		ShipmentRequest request = new ShipmentRequest(warehouseId, productId, qty);
		
		// when
		ShipmentConfirmation confirmation = service.requestShipment(request);
		
		// then
		assertNotNull(confirmation);
		assertEquals(qty, confirmation.getQty());
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(3, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}
	
	@Test 
	public void shouldCancelShipmentRequestAndNotTriggerAnAlert() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, 15);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		ShipmentLine line = new ShipmentLine(null, 1, new Product(productId, "", ""));
		Shipment shipment = new Shipment(
				new Warehouse(warehouseId, ""),
				Lists.newArrayList(line));
		
		// when
		service.cancelShipment(shipment);
		
		// then
		verify(repository).save(any(StockLevel.class));
		assertNull(listener.getAlert());
	}

	@Test 
	public void shouldCancelShipmentRequestAndTriggerAnAlert() {
		// given
		long productId = 3L;
		long warehouseId = 5L;
		StockLevel existingStockLevel = new StockLevel(warehouseId, productId, 3);
		
		given(repository.findByWarehouseIdAndProductId(warehouseId, productId)).willReturn(existingStockLevel);
		
		ShipmentLine line = new ShipmentLine(null, 13, new Product(productId, "", ""));
		Shipment shipment = new Shipment(
				new Warehouse(warehouseId, ""),
				Lists.newArrayList(line));
		
		// when
		service.cancelShipment(shipment);
		
		// then
		verify(repository).save(any(StockLevel.class));
		assertNotNull(listener.getAlert());
		StockAlert alert = listener.getAlert();
		assertEquals(productId, alert.getProductId());
		assertEquals(warehouseId, alert.getWarehouseId());
		assertEquals(16, alert.getStockLevel());
		assertEquals(5, alert.getThreshold());
	}
}
