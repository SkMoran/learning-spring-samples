package learning.spring.binarytea.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import learning.spring.binarytea.controller.request.NewOrderForm;
import learning.spring.binarytea.controller.request.StatusForm;
import learning.spring.binarytea.model.MenuItem;
import learning.spring.binarytea.model.Order;
import learning.spring.binarytea.model.OrderStatus;
import learning.spring.binarytea.service.MenuService;
import learning.spring.binarytea.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private MenuService menuService;

    @ModelAttribute("items")
    public List<MenuItem> items() {
        return menuService.getAllMenu();
    }

    @GetMapping
    public ModelAndView orderPage() {
        return new ModelAndView("order")
                .addObject(new NewOrderForm())
                .addObject("orders", orderService.getAllOrders());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Order> listOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String createNewOrder(@Valid NewOrderForm form, BindingResult result,
                                 ModelMap modelMap) {
        if (result.hasErrors()) {
            modelMap.addAttribute("orders", orderService.getAllOrders());
            return "order";
        }
        createOrder(form);
        modelMap.addAttribute("orders", orderService.getAllOrders());
        return "order";
    }

    @ResponseBody
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> createNewOrder(@RequestBody @Valid NewOrderForm form,
                                                BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(null);
        }
        Order order = createOrder(form);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
        return ResponseEntity.created(uri).body(order);
    }

    @ResponseBody
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Order> modifyOrderStatus(@RequestBody @Valid StatusForm form,
                                                  BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(null);
        }
        log.info("计划将ID={}的订单状态更新为{}", form.getId(), form.getStatus());
        OrderStatus status = OrderStatus.valueOf(form.getStatus());
        if (status == null) {
            log.warn("状态{}不正确", form.getStatus());
            return ResponseEntity.badRequest().body(null);
        }
        Order order = orderService.modifyOrderStatus(form.getId(), status);
        return order == null ?
                ResponseEntity.badRequest().body(null) : ResponseEntity.ok(order);
    }

    @ResponseBody
    @GetMapping("/{id}")
    @SentinelResource("query-order")
    public ResponseEntity<Order> queryOneOrder(@PathVariable("id") Long id) {
        Optional<Order> result = orderService.queryOrder(id);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private Order createOrder(NewOrderForm form) {
        List<MenuItem> itemList = form.getItemIdList().stream()
                .map(i -> NumberUtils.toLong(i))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> menuService.getByIdList(list)));
        Order order = orderService.createOrder(itemList, form.getDiscount());
        log.info("创建新订单，Order={}", order);
        return order;
    }
}
