function deleteContact(cid){
  		swal({
  		  title: "Are you sure?",
  		  text: "You want to delete this contact!!",
  		  icon: "warning",
  		  buttons: true,
  		  dangerMode: true,
  		})
  		.then((willDelete) => {
  		  if (willDelete) {
  		    
  			  window.location="/user/delete/"+cid;
  		    
  		  } else {
  		    swal("Your Contact is safe!");
  		  }
  		});
  	}