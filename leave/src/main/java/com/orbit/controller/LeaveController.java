package com.orbit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbit.models.Holiday;
import com.orbit.models.Leave;
import com.orbit.constants.LeaveStatus;
import com.orbit.repository.HolidayRepository;
import com.orbit.repository.LeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/leave")
public class LeaveController {
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HolidayRepository holidayRepository;
    List<Holiday> holidayList;
    @PostMapping()
    public Leave createLeave(@RequestBody Leave leave)
    {
      //  double numOfLeaves = getActualNumberOfLeaves(leave);
       // leave.setNumberOfLeaves(leave);
        Boolean chekkPreviouslyApply=checkLeaveApplied(leave);
        leave.setLeaveStatus(LeaveStatus.Applied.toString());
        leave.setCreationTime(LocalDate.now());
        leave.setUpdateTime(LocalDate.now());
        if(chekkPreviouslyApply==false){
            return leaveRepository.save(leave);
        }
        else{
            throw new UnsupportedOperationException("Already Applied1");
        }

    }

    @GetMapping("/{id}")
    public ResponseEntity<Leave> getLeaveById(@PathVariable(value = "id") Long leaveId)
    {
        try
        {
            Leave leave = leaveRepository.findById(leaveId).get();
            return ResponseEntity.ok().body(leave);
        }
        catch (Exception e)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/")
    public List<Leave> getAllLeave()
    {
        return leaveRepository.findAll();
    }

    @GetMapping("/leavebystatus")
    public List<Leave> getAllLeaveByStatus(@RequestParam("employeeId") Long employeeId, @RequestParam("leaveStatus") String leaveStatus)
    {
        return leaveRepository.findAllByLeaveStatus(employeeId, leaveStatus.toString());
    }
    @GetMapping("/leavebymanagerid")
    public List<Leave> getAllLeaveByManagerId(@RequestParam("managerId") Long managerId)
    {
        return leaveRepository.findAllByManagerId(managerId);
    }

    @GetMapping("/leavebystatusformanager")
    public List<Leave> getAllLeaveByStatusForManager(@RequestParam("managerId") Long managerId, @RequestParam("leaveStatus") LeaveStatus leaveStatus)
    {
        return leaveRepository.findAllByLeaveForManagerByStatus(managerId, leaveStatus.toString());
    }

    @GetMapping("/leavecountbystatusformanager")
    public Long getLeaveCountByStatusForManager(@RequestParam("managerId") Long managerId, @RequestParam("leaveStatus") LeaveStatus leaveStatus)
    {
        return leaveRepository.getLeaveCountByStatusForManager(managerId, leaveStatus.toString());
    }
    @GetMapping("/leavebyemployeeid")
    public List<Leave> getAllLeaveByEmployeeId(@RequestParam("employeeId") Long employeeId)
    {
        return leaveRepository.findAllByEmployeeId(employeeId, LocalDate.now().minusMonths(3));
    }
    @PatchMapping("/")
    public ResponseEntity<List<Leave>> updateLeave(@RequestBody List<Leave> leavePatch)
    {
        try
        {
            leavePatch.forEach(leave -> {
                Leave getleave = leaveRepository.findById(leave.getId()).get();
                getleave.setLeaveStatus(leave.getLeaveStatus());
                getleave.setRemark(leave.getRemark());
                getleave.setUpdateTime(LocalDate.now());
                leaveRepository.save(getleave);
            });

            return ResponseEntity.ok().body(leavePatch);
        }
        catch (Exception ex)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Leave> deleteLeave(@PathVariable(value = "id") Long leaveId)
    {
        try
        {
            Leave leave = leaveRepository.findById(leaveId).get();
            leaveRepository.delete(leave);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    private double getActualNumberOfLeaves(Leave leave)
    {
        holidayList = holidayRepository.findAll();

        int holidayCount = 0;
        for(Holiday holiday : holidayList)
        {
            if( holiday.getDate().compareTo(leave.getStartDate()) >= 0 &&
                holiday.getDate().compareTo(leave.getEndDate()) <= 0)
            {
                holidayCount++;
            }
        }

        return (double)ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate())
                + 1 - holidayCount;
    }

    private Boolean checkLeaveApplied(Leave leave){
     boolean checkApplied=false;
     int count=0;
        List<LocalDate> totalDates = new ArrayList<>();
        Leave leave1=new Leave();
        leave1.setStartDate(leave.getStartDate());
        leave1.setEndDate(leave.getEndDate());
        while (!leave1.getStartDate().isAfter(leave1.getEndDate())) {
            totalDates.add(leave1.getStartDate());
            leave1.setStartDate(leave1.getStartDate().plusDays(1));
        }
        List<Leave> EmpleaveData=leaveRepository.findAllByEmpId(leave.getEmployeeId());
        EmpleaveData.forEach(emp->{
            totalDates.forEach(dt->{
                if(emp.getLeaveStatus()!=LeaveStatus.Rejected.toString()){
                    if(emp.getStartDate().compareTo(dt)*dt.compareTo(emp.getEndDate())<0){
                        throw new UnsupportedOperationException("Already Applied");
                    }
                }
            });
        });
        return checkApplied;
    }
}
