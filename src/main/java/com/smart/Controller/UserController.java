package com.smart.Controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Massege;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model m,Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME:" +userName);
		
		
		//get the user using username(Email)
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER "+user);
		
		m.addAttribute("user",user);
	}
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model m,Principal principal) {
		
		m.addAttribute("title", "User Dashboard");
		
		
		return "normal/user_dashboard";
	}
	
	
	//open add form handler
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model m) {
		
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact",new Contact());
		
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String procesContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal, HttpSession session
			) {
		
		try {
		String name= principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		//processing and uploading file
		if(file.isEmpty()) {
			//if the file is empty then message
			System.out.println("File is empty");
			contact.setImage("contact.png");
		}
		else {
			//upload the the file to folder and update the name to contact
			contact.setImage(file.getOriginalFilename());
			
			File file2 = new ClassPathResource("static/img").getFile();
			
			Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
			
			System.out.println("Image is uploaded");
		}
		
		user.getContacts().add(contact);
		
		contact.setUser(user);
		
		this.userRepository.save(user);
		
		
		System.out.println("DATA"+contact);
		
		//message success......
		session.setAttribute("message", new Massege("Contact is added !! Add more","success"));
		
		System.out.println("Added to data base");
		}catch (Exception e) {
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			//message error
			session.setAttribute("message", new Massege("Something Went Wrong!! Try again","danger"));
		}
		return "normal/add_contact_form";
	}
	
	
	//show contacts handler
	//per page =5[n]
	//current page= 0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m, Principal principal) {
		
		m.addAttribute("title","Show User Contacts");
		//contact ki list bhejni hai
		String userName=principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
//		List<Contact> contacts = user.getContacts();
		
		//pageable has two arguments
		//current page
		//contact per page-5
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing contact detail handler
	
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model m,Principal principal) {
		
		System.out.println("CID"+cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//security check 
		//another user can't access others contact
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			
			m.addAttribute("contact",contact);
			m.addAttribute("title",contact.getName());
		}
		
		
		
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId,Model m,Principal principal,HttpSession session) {
		
		try {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//check...
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
		//contact.setUser(null);
		
			user.getContacts().remove(contact);
			
			this.userRepository.save(user);
		//remove 
		//img
		//contact.image;
		File deleteFile = new ClassPathResource("static/img").getFile();
		File file1=new File(deleteFile,contact.getImage());
		file1.delete();
		
		
		
		
		//	this.contactRepository.delete(contact);
		session.setAttribute("message", new Massege("Contact deleted successfully..", "success"));
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model m) {
		
		
		m.addAttribute("title","update contact");
		Contact contact = this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	
	//update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model m,HttpSession session,Principal principal) {
		
		
		try {
			
			//old contact details
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			
			//image
			if(!file.isEmpty()) {
				//file work
				//rewrite
				
				//delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldContactDetail.getImage());
				file1.delete();
				
				//update new photo
				
				File file2 = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path , StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
			}else {
				contact.setImage(oldContactDetail.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Massege("Your contact is updated...", "success") );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("CONTACT"+ contact.getName());
		System.out.println("ID"+contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model m) {
		
		m.addAttribute("title","Your Profile");
		return "normal/profile";
	}
	
	//open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		
		
		return "normal/settings";
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldpassword") String oldpassword,@RequestParam("newpassword") String newpassword ,Principal principal,HttpSession session) {
		
		System.out.println("OLD PASSWORD"+oldpassword);
		System.out.println("NEW PASSWORD"+newpassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		
		if(this.bCryptPasswordEncoder.matches(oldpassword, newpassword)) {
			//change the password
			
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
			this.userRepository.save(currentUser);
			
			session.setAttribute("message", new Massege("Your password is successfully changed", "success"));
		
		}else {
			//error
			session.setAttribute("message", new Massege("Old password didn't matched !!", "danger"));
			return "redirect:/user/settings";
		
		}
		
		return "redirect:/user/index";
	}
}
