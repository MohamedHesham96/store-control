package com.blue.soft.store.controllers;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.blue.soft.store.entity.BillBuy;
import com.blue.soft.store.entity.BillReturn;
import com.blue.soft.store.entity.BillReturnItem;
import com.blue.soft.store.entity.BillSell;
import com.blue.soft.store.entity.Item;
import com.blue.soft.store.entity.TempBillItem;
import com.blue.soft.store.service.BillReturnItemsService;
import com.blue.soft.store.service.BillReturnService;
import com.blue.soft.store.service.ClientService;
import com.blue.soft.store.service.ItemService;
import com.blue.soft.store.service.TempBillItemsService;

@Controller
public class UpdateBillReturnController {

	@Autowired
	HttpSession httpSession;

	@Autowired
	ItemService itemService;

	@Autowired
	BillReturnService billReturnService;

	@Autowired
	ClientService clientService;

	@Autowired
	BillReturnItemsService billReturnItemsService;

	@Autowired
	TempBillItemsService tempBillItemsService;

	// بيشوف لو في فاتورة بتتعدل بالفعل وبيدخل عليها لو كده
	@RequestMapping("/change-return-bill-to-update")
	public String changeReturnBillToUpdate(@RequestParam(name = "returnBillId") String returnBillId, Model theModel) {

		BillReturn billReturn = billReturnService.getBillReturnById(returnBillId);

		if (billReturn.isUpdateNow())
			return "redirect:/show-update-return-bill";

		List<BillReturnItem> billReturnItemsList = billReturn.getBillReturnItems();

		billReturn.setUpdateNow(true);

		billReturnService.saveReturnBill(billReturn);

		tempBillItemsService.addBillReturnItems(billReturnItemsList);

		return "redirect:/show-update-return-bill";
	}

	// بيعرض صفحة التعديل على الفاتورة
	@RequestMapping("/show-update-return-bill")
	public String showUpdateReturnBill(Model theModel) {

		BillReturn billReturn = billReturnService.getBillReturnByUpdateNow();

		float total = billReturn.getTotal();

		billReturnService.saveReturnBill(billReturn);

		theModel.addAttribute("total", total);
		theModel.addAttribute("item", new Item());
		theModel.addAttribute("billReturn", billReturn);
		theModel.addAttribute("updateItem", new BillReturnItem());
		theModel.addAttribute("itemsList", itemService.getAllItems());

		return "update-return-bill";
	}

	// بيضيف صنف للفاتورة اللي بتتعدل
	@RequestMapping("/add-item-to-update-return-bill")
	public String addToUpdateReturnBill(@ModelAttribute(name = "item") Item item, Model theModel) throws Exception {

		BillReturnItem billReturnItem = new BillReturnItem();
		Item theItem = itemService.getItemById(item.getId());

		if (item.getQuantity() < theItem.getQuantity() && item.getQuantity() > 0) {

			// String billId = httpSession.getAttribute("billReturnId").toString();
			// BillReturn billReturn = billReturnService.getBillReturnById(billId);

			BillReturn billReturn = billReturnService.getBillReturnByUpdateNow();

			billReturnItem.setItem(theItem);
			billReturnItem.setBillReturn(billReturn);
			billReturnItem.setReturnPrice(theItem.getSellPrice());
			billReturnItem.setQuantity(item.getQuantity());

			billReturnItemsService.addBillReturnItem(billReturnItem);

		} else {

			throw new Exception("Quantity is Not Good !");
		}

		return "redirect:/show-update-return-bill";
	}

	// بيسمح اصناف من الفاتورة اللي بتتعدل
	@RequestMapping("/delete-returnBillItemUpdate")
	public String deleteReturnBillItemUpdate(@RequestParam(name = "returnBillItemId") String returnBillItemId) {

		billReturnItemsService.deleteBillReturnItem(returnBillItemId);

		return "redirect:/show-update-return-bill";

	}

	@RequestMapping("/update-returnBill")
	public String updateReturnBill(@RequestParam(name = "returnBillId") String returnBillId,
			RedirectAttributes attributes) {

		BillReturn billReturn = billReturnService.getBillReturnById(returnBillId);

		billReturn.setUpdateNow(false);

		billReturnService.saveReturnBill(billReturn);

		clearTempBillItems(billReturn);

		return "redirect:/show-return-bill-list";

	}

	// بيجيب الداتا من الفورم بتاعت التعديل علشان يعدل على الاصناف
	@RequestMapping("/update-returnBillItem")
	public String deleteReturnBillItemUpdate(@ModelAttribute(name = "updateItem") Item returnBillItem) {

		BillReturnItem oldBRItem = billReturnItemsService.getBillReturnItem(returnBillItem.getId());

		oldBRItem.setQuantity(returnBillItem.getQuantity());
		oldBRItem.setReturnPrice(returnBillItem.getSellPrice());

		billReturnItemsService.addBillReturnItem(oldBRItem);

		return "redirect:/show-update-return-bill";

	}

	@RequestMapping("/retrive-UpdateReturnBill")
	public String retriveUpdateReturnBill(@RequestParam(name = "returnBillId") String returnBillId, Model theModel) {

		BillReturn billReturn = billReturnService.getBillReturnById(returnBillId);

		List<String> ids = billReturn.getBillReturnItemsIDS();

		int size = billReturn.getBillReturnItems().size();

		for (int i = size - 1; i >= 0; i--) {

			billReturn.removeItem(billReturn.getBillReturnItems().get(i));
		}

		for (String id : ids) {

			billReturnItemsService.deleteBillReturnItem(id);

		}

		List<TempBillItem> tempBillItemsList = tempBillItemsService.getTempBillItems(billReturn.getId(), "returnBill");

		for (TempBillItem tempBillItem : tempBillItemsList) {

			Item theItem = itemService.getItemById(tempBillItem.getItemId());

			billReturn.addBillReturnItem(
					new BillReturnItem(billReturn, theItem, tempBillItem.getQuantity(), tempBillItem.getPrice()));

		}

		billReturnService.saveReturnBill(billReturn);

		return "redirect:/show-update-return-bill";
	}

	@RequestMapping("/delete-updateReturnBill")
	public String deleteReturnBill(@RequestParam(name = "returnBillId") String returnBillId) {

		BillReturn billReturn = billReturnService.getBillReturnById(returnBillId);

		billReturnService.deleteReturnBill(returnBillId);

		clearTempBillItems(billReturn);

		return "redirect:/show-return-bill-info";

	}

	public void clearTempBillItems(BillReturn billReturn) {

		List<TempBillItem> tempBillItemsList = tempBillItemsService.getTempBillItems(billReturn.getId(), "returnBill");

		for (TempBillItem tempBillItem : tempBillItemsList) {

			tempBillItemsService.deleteTempBillItems(tempBillItem.getId());

		}
	}
}