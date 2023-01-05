import { Menu, MenuItem } from "@material-ui/core";
import React, { useState, useEffect } from "react";

export default function NodeContextMenu(props) {
  const [anchorEl, setAnchorEl] = useState(props ? props.anchor : null);

  const handleClose = () => {
    setAnchorEl(null);
  };
  return (
    <Menu
      id="simple-menu"
      anchorEl={anchorEl}
      keepMounted
      open={Boolean(anchorEl)}
      onClose={handleClose}
      style={{ zIndex: 4, top: props.y, left: props.x }}
    >
      <MenuItem onClick={handleClose}>Profile</MenuItem>
      <MenuItem onClick={handleClose}>My account</MenuItem>
      <MenuItem onClick={handleClose}>Logout</MenuItem>
    </Menu>
  );
}
